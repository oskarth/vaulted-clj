(ns vaulted-clj.util)

(def +email-regex+
  "Regex for email. From http://www.regular-expressions.info/email.html"
  #"[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?")

(def pattern #"([a-z]+)\.([a-z_]+)")
(defn nested? [s] (re-find pattern s))

(defn parse-nested
  "If field string represent a object, turn it into a map.
   Calling with \"address.postal_code\") evaluates to {:address (:postal_code)}"
  [str]
  (if (nested? str)
    ((fn [[_ key val]] {(keyword key) (list (keyword val))})
     (nested? str))))

(defn parse-flat
  "If field string isn't nested, return as a keyword."
  [str]
  (if (not (nested? str))
    (keyword str)))

(defn strings->fields
  "Turns required fields strings into Clojure fields."
  [reqs]
  (let [flats (into [] (filter #(not (nil? %)) (map parse-flat reqs)))
        nests (apply merge-with into (map parse-nested reqs))]
    (cons nests flats)))

(comment

  ;; example usage of strings-> fields
  (strings->fields ["name" "address.line1" "address.city" "address.postal_code" "address.country_code"])
  ;; => ({:address (:country_code :postal_code :city :line)} :name)

  )
