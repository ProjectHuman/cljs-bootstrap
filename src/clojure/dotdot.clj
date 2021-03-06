(ns dotdot
  (:require [clojure.java.io :as io]
            [cljs.env :as env]
            [cljs.analyzer :as ana]
            [cljs.compiler :as comp]
            [cljs.closure :as closure]
            [cljs.tagged-literals :as tags]
            [clojure.tools.reader :as r]
            [clojure.tools.reader.reader-types :refer [string-push-back-reader]]))

(def cenv (env/default-compiler-env))

(comment
  (env/with-compiler-env cenv
    (let [src (io/resource "cljs/core.cljc")]
      (closure/compile src
        {:output-file (closure/src-file->target-file src)
         :force       true
         :mode        :interactive})))

  (env/with-compiler-env cenv
    (comp/munge
      (ana/resolve-var {:ns {:name 'cljs.core$macros}}
        'cljs.core$macros/..)))

  (def f (slurp (io/resource "cljs/core.cljs")))

  (string? f)

  ;; ~42ms on work machine
  (time
    (let [rdr (string-push-back-reader f)
          eof (Object.)]
      (binding [*ns* (create-ns 'cljs.analyzer)
                r/*data-readers* tags/*cljs-data-readers*]
        (loop []
          (let [x (r/read {:eof eof} rdr)]
            (when-not (identical? eof x)
              (recur)))))))

  ;; ~830ms
  (dotimes [_ 10]
    (time (ana/analyze-file "cljs/core.cljs")))

  ;; 2.2s
  (dotimes [_ 10]
    (time
      (env/ensure
        (closure/compile-form-seq
          (ana/forms-seq*
            (io/reader (io/resource "cljs/core.cljs")))))))
  )
