(ns vaulted-clj.validation
  (:use vaulted-clj.core)
  (:require [schema.core :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wrapper functions for simple data validation

(defn customer? [customer]
  (s/validate Customer customer))

(defn address? [address]
  (s/validate Address address))

(defn item? [item]
  (s/validate Item item))

(defn debit? [debit]
  (s/validate Debit debit))

(defn email? [email]
  (s/validate Email email))
