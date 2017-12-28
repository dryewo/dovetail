# cyrus-logging

[![Build Status](https://travis-ci.org/dryewo/cyrus-logging.svg?branch=master)](https://travis-ci.org/dryewo/squeeze)
[![codecov](https://codecov.io/gh/dryewo/cyrus-logging/branch/master/graph/badge.svg)](https://codecov.io/gh/dryewo/squeeze)
[![Clojars Project](https://img.shields.io/clojars/v/cyrus/logging.svg)](https://clojars.org/cyrus/logging)

Companion logging library for [Cyrus] Leiningen template.

## Dependencies

WARNING! This library carries an opinion.
It brings the following dependencies:

* [Timbre]
* [slf4j-timbre]
* [clojure.tools.logging]

## Usage

```edn
[cyrus/logging "0.1.0"]
```

All the examples bellow assume:
```clj
(require '[cyrus-logging.core :as log])
```

### Encoding of %s as pr-str

Default behavior:

```clj
(timbre/infof "%s" (take 5 (range)))
 INFO [nREPL-worker-42] c-l.c-test - clojure.lang.LazySeq@1b554e1
(timbre/infof "%s" "123")
 INFO [nREPL-worker-42] c-l.c-test - foo
(timbre/infof "%s" 123)
 INFO [nREPL-worker-42] c-l.c-test - 123
(timbre/infof "%s" "")
 INFO [nREPL-worker-42] c-l.c-test - 
(timbre/infof "%s" nil)
 INFO [nREPL-worker-42] c-l.c-test - null
```

cyrus-logging behavior:

```clj
(log/info "%s" (take 5 (range)))
 INFO [nREPL-worker-42] c-l.c-test - (0 1 2 3 4)
(log/info "%s" "123")
 INFO [nREPL-worker-42] c-l.c-test - "123"
(log/info "%s" 123)
 INFO [nREPL-worker-42] c-l.c-test - 123
(log/info "%s" "")
 INFO [nREPL-worker-42] c-l.c-test - ""
(log/info "%s" nil)
 INFO [nREPL-worker-42] c-l.c-test - nil
```

Encoding function can be replaced:

```clj
(log/set-%s-encoder! json/encode)

(log/info "%s" (take 5 (range)))
 INFO [nREPL-worker-42] c-l.c-test - [0,1,2,3,4]
(log/info "%s" "123")
 INFO [nREPL-worker-42] c-l.c-test - "123"
(log/info "%s" 123)
 INFO [nREPL-worker-42] c-l.c-test - 123
(log/info "%s" "")
 INFO [nREPL-worker-42] c-l.c-test - ""
(log/info "%s" nil)
 INFO [nREPL-worker-42] c-l.c-test - null
(log/info "%s" {:foo "bar"})
 INFO [nREPL-worker-42] c-l.c-test - {"foo":"bar"}
```

### Default output function

Default output function can be enabled as:

```clj
(log/set-output-fn! log/default-log-output-fn)
```

* no timestamp
* space-padded level
* content of `:request` key of the context
* current thread name
* abbreviated namespace
* formatted message
* stacktrace of the throwable (if present) — on a new line

### Filtering by namespace

Sometimes we need `:info` logging level, but want to exclude some very chatty library from the output.


```clj
(log/set-level! :info)
(log/set-ns-log-levels!
  {"chatty.library.*" :warn})
```
Or, we might want to enable our namespaces to output `:debug`, but leave others at `:info`:

```clj
(log/set-level! :debug)
(log/set-ns-log-levels!
  {"our.app.*" :debug
   :all        :info})
```

In general, effective level is the higher of overall and per-namespace:

```clj
;; Equivalent to :info overall
(log/set-level! :debug)
(log/set-ns-log-levels!
  {:all :info})

;; Same
(log/set-level! :info)
(log/set-ns-log-levels!
  {:all :debug})
```

If a namespace matches several selectors, then the most specific one takes effect:

```clj
;; for our.app.ns effectibe level will be :info
(log/set-ns-log-levels!
  {"our.app.*" :info
   "our.*"     :debug})
```

### Throwable as first argument

All helper functions (not only `error`) support `Throwable` as first argument:

```
(log/info (Exception. "HAHA") "Something bad happened")
 INFO [nREPL-worker-42] c-l.c-test - Something bad happened
                clojure.core/eval       core.clj: 3206
                              ...                     
                   user/eval15501      REPL Input     
                              ...                     
cyrus-logging.core-test/eval15508  core_test.clj:   92
java.lang.Exception: HAHA
```

### Setting log level

If your application supports setting log level via an environment variable,
it's better to be flexible and accept both `LOG_LEVEL=debug` and `LOG_LEVEL=DEBUG`.

cyrus-logging also includes `set-level!` function, so that you don't have to explicitly require `taoensso.timbre` namespace.

```clj
(defn -main [& args]
  ;; Default log level
  (log/set-level! :info)
  ;; Will do nothing if LOG_LEVEL is not set
  (log/set-log-level-from-env! (System/getenv "LOG_LEVEL"))
  ...)
```

### No colors in the output

Timbre includes [pretty], which makes stacktraces look pretty colorful in the terminal.
However, this might get in the way if you collect logs from the stdout and aggregate them for viewing later.

If you call `disable-console-logging-colors` from `-main`, it will only affect production runs, but will let you
enjoy colorful stacktraces when REPL-driven developing:

```clj
(defn -main [& args]
  (log/disable-console-logging-colors)
  ...)
```

## License

Copyright © 2017 Dmitrii Balakhonskii

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[Cyrus]: https://github.com/dryewo/cyrus
[Timbre]: https://github.com/ptaoussanis/timbre
[slf4j-timbre]: https://github.com/fzakaria/slf4j-timbre
[Cheshire]: https://github.com/dakrone/cheshire
[clojure.tools.logging]: https://github.com/clojure/tools.logging
[pretty]: https://github.com/AvisoNovate/pretty
