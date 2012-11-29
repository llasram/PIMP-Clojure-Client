(ns pimp.util
  (:require [clojure.reflect]
            [clojure.string :as str]))

;; Stolen from clojure/core.clj
(defmacro assert-args [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                 (str (first ~'&form) " requires " ~(second pairs) " in "
                      ~'*ns* ":" (:line (meta ~'&form))))))
     ~(let [more (nnext pairs)]
        (when more
          (list* `assert-args more)))))

(defmacro doto-let
  "bindings => binding-form expr

Evaluates expr, and evaluates body with its result bound to the binding-form.
Returns the result of expr."
  [bindings & body]
  (assert-args
      (vector? bindings) "a vector for its binding"
      (= 2 (count bindings)) "exactly 2 forms in binding vector")
  (let [[bf expr] bindings]
    `(let [value# ~expr, ~bf value#] ~@body value#)))

(defmacro returning
  "Evaluates the result of expr, then evaluates the forms in body (presumably
for side-effects), then returns the result of expr."
  [expr & body] `(let [value# ~expr] ~@body value#))

(defn into-by
  [f m xs] (persistent! (reduce #(assoc! %1 (f %2) %2) (transient m) xs)))

(defn ^:private default-methods
  [class & this*]
  (let [params #(->> % :parameter-types (concat this*)
                     (map (fn [_] (gensym))) vec)]
    (->> class resolve clojure.reflect/reflect :members
         (filter (comp :abstract :flags))
         (map (comp seq (juxt :name params)))
         (into-by first {}))))

(defmacro reify+
  [iface & forms]
  (let [methods (default-methods iface 'this)]
    `(reify ~iface ~@(vals (into-by first methods forms)))))

(defmacro proxy+
  [[class] args & forms]
  (let [methods (default-methods class)]
    `(proxy [~class] ~args ~@(vals (into-by first methods forms)))))

(defn keywordize-camel
  [camel]
  (->> camel (partition-by #(Character/isUpperCase %)) (partition 2)
       (map (comp #(.toLowerCase %) (partial apply str) (partial apply concat)))
       (str/join "-") keyword))
