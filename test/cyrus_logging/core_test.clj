(ns cyrus-logging.core-test
  (:require [clojure.test :refer :all]
            [cyrus-logging.core :refer :all]
            [clojure.string :as str]
            [taoensso.timbre :as timbre])
  (:import (clojure.lang ExceptionInfo)))

(set-output-fn! default-log-output-fn)
(disable-console-logging-colors)

(use-fixtures
  :each (fn [f]
          (set-level! :info)
          (set-ns-log-levels! {})
          (set-%s-encoder! pr-str)
          (f)))

(deftest helper-functions-obey-levels
  (testing "All functions obey levels"
    (are [?level ?count]
      (do
        (set-level! ?level)
        (is (= ?count
               (->> [(with-out-str (error ""))
                     (with-out-str (warn ""))
                     (with-out-str (info ""))
                     (with-out-str (debug ""))
                     (with-out-str (trace ""))]
                    (remove str/blank?)
                    (count)))))
      :trace 5
      :debug 4
      :info 3
      :warn 2
      :error 1)))

(deftest helper-functions-support-throwables
  (testing "All helper functions support throwables as first argument"
    (set-level! :trace)
    (is (re-seq #"java.lang.Exception: haha" (with-out-str (error (Exception. "haha") ""))))
    (is (re-seq #"java.lang.Exception: haha" (with-out-str (warn (Exception. "haha") ""))))
    (is (re-seq #"java.lang.Exception: haha" (with-out-str (info (Exception. "haha") ""))))
    (is (re-seq #"java.lang.Exception: haha" (with-out-str (debug (Exception. "haha") ""))))
    (is (re-seq #"java.lang.Exception: haha" (with-out-str (trace (Exception. "haha") ""))))))

(deftest helper-functions-format
  (testing "All helper functions format %s as pr-str"
    (set-level! :trace)
    (is (re-seq #"\"str\" \[1\] \{:a 1\}\n" (with-out-str (error "%s %s %s" "str" [1] {:a 1}))))
    (is (re-seq #"\"str\" \[1\] \{:a 1\}\n" (with-out-str (warn "%s %s %s" "str" [1] {:a 1}))))
    (is (re-seq #"\"str\" \[1\] \{:a 1\}\n" (with-out-str (info "%s %s %s" "str" [1] {:a 1}))))
    (is (re-seq #"\"str\" \[1\] \{:a 1\}\n" (with-out-str (error "%s %s %s" "str" [1] {:a 1}))))
    (is (re-seq #"\"str\" \[1\] \{:a 1\}\n" (with-out-str (trace "%s %s %s" "str" [1] {:a 1})))))
  (testing "Can change formatting function"
    (set-%s-encoder! #(apply str (reverse (pr-str %))))
    (is (re-seq #" INFO \[.+\] c-l.c-test - \"rts\" \]1\[ \}1 a:\{"
                (with-out-str (info "%s %s %s" "str" [1] {:a 1}))))))

(deftest shortening-namespaces
  (testing "Namespaces are shortened"
    (set-level! :info)
    (is (re-seq #" INFO \[.+\] c-l.c-test - Hello\n"
                (with-out-str (info "Hello"))))))

(deftest setting-level
  (testing "When level matches, output is present"
    (set-level! :info)
    (is (not (str/blank? (with-out-str (info "Hello"))))))
  (testing "When level is above, no output"
    (set-level! :warn)
    (is (str/blank? (with-out-str (info "Hello"))))))

(deftest namespace-filtering
  (testing "Of overall level and per-namespace level the higher takes effect"
    (set-ns-log-levels! {"cyrus-logging.*" :info})
    (set-level! :debug)
    (is (str/blank? (with-out-str (debug "Hello")))))
  (testing "Of overall level and per-namespace level the higher takes effect"
    (set-ns-log-levels! {"cyrus-logging.*" :info})
    (set-level! :debug)
    (is (str/blank? (with-out-str (debug "Hello"))))))

(deftest setting-from-env
  (testing "Valid log levels are accepted"
    (are [?level-name ?expected-level]
      (do
        (set-log-level-from-env! ?level-name)
        (is (= ?expected-level (:level timbre/*config*))))
      "INFO" :info
      "debug" :debug
      :error :error
      'trace :trace))
  (testing "nil does nothing"
    (set-level! :trace)
    (set-log-level-from-env! nil)
    (is (= :trace (:level timbre/*config*))))
  (testing "Invalid log levels are not accepted"
    (is (thrown? ExceptionInfo (set-log-level-from-env! "")))
    (is (thrown? ExceptionInfo (set-log-level-from-env! "foo")))))
