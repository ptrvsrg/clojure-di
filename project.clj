(defproject clojure-di "0.0.0"
  :description "A Clojure library for DI container"
  :url "https://github.com/ptrvsrg/clojure-di"
  :license {:name "Apache 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.12.4"]]
  :plugins [[lein-cloverage/lein-cloverage "1.2.4"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.1"]]}}
  :repl-options {:init-ns clojure-di.core})
