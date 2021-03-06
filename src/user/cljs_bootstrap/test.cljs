(ns cljs-bootstrap.test
  (:require [cljs.js :as cljs]))

(enable-console-print!)

(def vm (js/require "vm"))
(def fs (js/require "fs"))

(set! *target* "nodejs")
(def st (cljs/empty-state))

(defn node-eval [{:keys [name source]}]
  (.runInThisContext vm source (str (munge name) ".js")))

(defn node-load [{:keys [name]} cb]
  (when (= name 'hello-world.core)
    (let [path (str "src/user/" (cljs/ns->relpath name) ".cljs")]
      (.readFile fs path "utf-8"
        (fn [err src]
          (cb (if-not err
                {:lang :clj :source src}
                (.error js/console err))))))))

(comment
  (require-macros '[cljs.env.macros :as env])
  (require '[cljs.pprint :as pp]
           '[cljs.env :as env]
           '[cljs.analyzer :as ana]
           '[cljs.compiler :as comp])

  ;; works
  (cljs/eval st '(defn foo [a b] (+ a b))
    {:eval-fn cljs/js-eval}
    (fn [res]
      (println res)))

  ;; works
  (cljs/compile st "(defprotocol IFoo (foo [this]))"
    (fn [js-source]
      (println "Source:")
      (println js-source)))

  ;; works
  (cljs/eval-str st
    "(defn foo [a b] (+ a b))
     (defn bar [c d] (+ c d))"
    {:eval-fn cljs/js-eval}
    (fn [res]
      (println res)))

  ;; works
  (cljs/eval-str st "1"
    {:eval-fn cljs/js-eval
     :context :expr}
    (fn [res]
      (println res)))

  ;; works
  (cljs/eval-str st "(def x 1)"
    {:eval-fn cljs/js-eval
     :context :expr
     :def-emits-var true}
    (fn [res]
      (println res)))

  ;; works
  (cljs/eval st '(ns foo.bar)
    {:eval-fn cljs/js-eval}
    (fn [res]
      (println res)))

  (binding [cljs/*load-fn*
            (fn [{:keys [name]} cb]
              (println name)
              (cb {:lang :js
                   :source "function hello() { console.log(\"Hello!\"); };"}))]
    (cljs/compile st "(ns foo.bar (:require [hello-world.core]))"
      {:verbose true
       :source-map true}
      (fn [js-source]
        (println "Source:")
        (println js-source))))

  ;; works!
  (cljs/compile st "(defn foo\n[a b]\n(+ a b))" 'cljs.foo
    {:verbose true :source-map true}
    (fn [js-source]
      (println "Source:")
      (println js-source)))

  ;; bar compiled in the wrong namespace
  (cljs/eval-str st
    "(ns foo.bar (:require [hello-world.core]))\n(hello-world.core/bar 3 4)"
    'foo.bar
    {:verbose true
     :source-map true
     :eval-fn node-eval
     :load-fn node-load}
    (fn [ret]
      (println ret)))

  ;; sanity check
  (let [path (str "src/user/hello_world/core.cljs")]
    (.readFile fs path "utf-8"
      (fn [err src]
        (prn
          (if-not err
            {:lang :clj :source src}
            (.error js/console err))))))
  )