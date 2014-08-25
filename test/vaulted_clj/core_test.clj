(ns vaulted-clj.core-test
  (:require [clojure.test :refer :all]
            [vaulted-clj.core :refer :all]
            [vaulted-clj.util :as util]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [schema.core :as s]))

;; WARNING: Tests assumes VAULTED-API-KEY is in the environment.

;; Mocking
(defn gen-debit-map []
  {:number (str (rand-int 1000000))
   :items [{:amount 5000
            :vat_percent 12.5
            :description "Flicknex Streaming."
            :period {:start "1396232247" :end "1398824247"}
            :quantity 1}]
   :description ""})

(defn gen-customer []
  (post-resource (gen-customers-url :test) {:email "test@example.com"}))

(deftest test-internals
  (testing "gen-base-url"
    (is (= (gen-base-url nil) "https://test.vaulted.com/v0"))
    (is (= (gen-base-url :foo) "https://test.vaulted.com/v0"))
    (is (= (gen-base-url :live) "https://api.vaulted.com/v0")))

  (testing "with-token macro"
    (is (map? (with-token *vaulted-key*
                (post-resource (gen-customers-url :test)
                {:email "test@example.com"}))))))

(deftest test-exceptions
  (testing "request cannot be fulfilled due to bad syntax"
    (is (thrown? Exception
                 (post-resource (gen-customers-url :test) {:name "Foobar"})))
    (is (thrown? Exception
                 (get-resource (gen-customer-url "faulty" :test)))))

  (testing "authentication is possible but has failed"
    (is (thrown-with-msg?
         Exception #"access denied"
         (with-token ""
           (post-resource (gen-customers-url :test)
                          {:email "test@example.com"}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PUBLIC API TESTS

;; Mocking
(def cid (:id (post-customer {:email "devnull@vaulted.com"} :test)))
(def did (:id (post-debit cid (gen-debit-map) :test)))
(def rid (:reference (post-debit cid (gen-debit-map) :test))) ;; duplication
(def refund (:uri (post-debit cid (gen-debit-map) :test))) ;; duplication
(def refund-map {:debit_uri refund :amount 1900 :number "1000"})
(def credit-map {:amount 1900 :number "1000"})

(deftest test-customer
  (testing "post customer"
    (is (map? (post-customer {:email "devnull@vaulted.com"} :test))))
  
  (testing "get customer"
    (is (== (:_type (get-customer cid :test) "customer"))))

  (testing "get requirements"
    (is (not (nil? (get-requirements cid)))))
  ;; get-requirements
  ;; put-customer
  ;; get-customer-by-email
)

(deftest test-debit
  (testing "post debit"
    (is (map? (post-debit cid (gen-debit-map) :test))))

  (testing "get debit"
    (is (== (:_type (get-debit cid did :test) "debit"))))

  (testing "get debit by ref"
    (is (== (:_type (get-debit-by-ref cid rid :test) "debit"))))
)

(deftest test-refund
  (testing "post refund exception"
    (is (thrown-with-msg?
         Exception #"cannot refund until debit has succeeded"
         (post-refund cid refund-map :test))))

   ;; get-refund
   ;; get-refund-by-ref
)

(deftest test-credit
  (testing "post credit"
    (is (thrown-with-msg?
         Exception #"no such account*"
         (post-credit cid credit-map :test))))

  ;; get-credit
  ;; get-credit-by-ref
)

;; Schema tests

(deftest test-schemas
  (testing "Customer works"
    (is (= {:email "devnull@vaulted.io"}
           (s/validate Customer {:email "devnull@vaulted.io"})))
    (is (= {:email "devnull@vaulted.io" :name "Foobar"}
           (s/validate Customer {:email "devnull@vaulted.io" :name "Foobar"}))))
  (testing "Customer doesn't work"
    (is (thrown? Exception (s/validate Customer {:name "Foobar"})))))

;;  (run-tests)

