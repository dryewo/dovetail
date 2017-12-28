(ns dovetail.core
  (:require [taoensso.timbre :as timbre]
            [taoensso.encore :as enc]
            [io.aviso.exception :as aviso-ex]
            [clojure.string :as str]))

;; Logging with encoding

(def ^:dynamic *%s-encoder* pr-str)

(defn set-%s-encoder! [encoder-fn]
  (alter-var-root #'*%s-encoder* (constantly encoder-fn)))

(defn format-log-message
  "Similar to `format`, but uses encoded representation of values.
   Expects only %s in the format string."
  [fmt & args]
  (apply format fmt (map *%s-encoder* args)))

(defmacro logf
  "Logs a message, formatted with clear distinction for dynamic values."
  [level throwable-or-message & [maybe-message & rest-args :as more]]
  `(let [[throwable# message# & args#] (if (instance? Throwable ~throwable-or-message)
                                         [~throwable-or-message ~maybe-message ~@rest-args]
                                         [nil ~throwable-or-message ~@more])]
     (when (timbre/may-log? ~level)
       (let [msg# (apply format-log-message message# args#)]
         (timbre/log ~level throwable# msg#)))))

(defmacro trace [& args]
  `(logf :trace ~@args))

(defmacro debug [& args]
  `(logf :debug ~@args))

(defmacro info [& args]
  `(logf :info ~@args))

(defmacro warn [& args]
  `(logf :warn ~@args))

(defmacro error [& args]
  `(logf :error ~@args))

(defmacro with-context [& args]
  `(timbre/with-context ~@args))

;; Colorless logging

(defn make-colorless-appender [appender]
  (update appender :fn
          (fn [f]
            (fn [data]
              (binding [aviso-ex/*fonts* {}]
                (f data))))))

(defn disable-console-logging-colors []
  (timbre/merge-config! {:appenders {:println (make-colorless-appender
                                                (get-in timbre/example-config [:appenders :println]))}}))

;; Filtering by namespace

(defn ns-filter [fltr]
  (-> fltr enc/compile-ns-filter enc/memoize_))

(defn find-best-ns-pattern [ns-str ns-patterns]
  (some->> ns-patterns
           (filter #(and (string? %)
                         ((ns-filter %) ns-str)))
           not-empty
           (apply max-key count)))

(defn log-by-ns-pattern
  [ns-patterns & [{:keys [?ns-str level] :as opts}]]
  (let [best-ns-pattern       (or (find-best-ns-pattern ?ns-str (keys ns-patterns))
                                  :all)
        best-ns-pattern-level (get ns-patterns best-ns-pattern :trace)]
    (when (timbre/level>= level best-ns-pattern-level)
      opts)))

(defn set-ns-log-levels! [log-ns-map]
  (timbre/merge-config! {:middleware [(partial log-by-ns-pattern log-ns-map)]}))

;; Setting log levels

(defn set-level! [level]
  (timbre/set-level! level))

(defn canonical-level-name [strname]
  (-> strname
      (name)
      (str/lower-case)
      (keyword)))

(defn set-log-level-from-env! [level-name]
  (some-> level-name
          (canonical-level-name)
          (timbre/valid-level)
          (set-level!)))

;; Logging output format with abbreviated namespaces

;; From https://github.com/alexander-yakushev/ns-graph/blob/master/src/ns_graph/core.clj
(defn abbrev-name
  "Abbreviate a dot- and dash- separated string by first letter. Leave the last
  part intact unless `abbr-last` is true."
  [string & [abbr-last]]
  (let [parts (partition-by #{\. \-} string)]
    (str/join
      (if abbr-last
        (map first parts)
        (concat (map first (butlast parts)) (last parts))))))

(defn default-log-output-fn
  "Formatting function for all log output."
  ([data]
   (default-log-output-fn nil data))
  ([_ data]
   (let [{:keys [level ?ns-str ?msg-fmt vargs ?err context]} data]
     (format "%5s [%s]%s %s - %s%s"
             (str/upper-case (name level))
             (.getName (Thread/currentThread))
             (if context (str " " context) "")
             (abbrev-name ?ns-str)
             (if-let [fmt ?msg-fmt]
               (apply format fmt vargs)
               (apply str vargs))
             (if ?err (str "\n" (timbre/stacktrace ?err)) "")))))

(defn set-output-fn! [output-fn]
  (timbre/merge-config! {:output-fn output-fn}))
