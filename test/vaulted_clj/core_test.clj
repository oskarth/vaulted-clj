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
   :description ""}]


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

(deftest test-post-resource
  (let [customer (post-resource (gen-customers-url :test)
                                {:email "test@example.com"})
        debit (gen-debit-map)]
    (testing "post-resource"
      (is (map? (post-resource (gen-customers-url :test)
                               {:email "test@example.com"}))))

    (testing "post-resource with post-debit")
    (is (post-resource (gen-debit-url (:id customer)) debit))))

(deftest test-put
  (let [existing-id (:id (post-resource (gen-customers-url :test)
                                        {:email "existing@example.com"}))]
    (testing "put-resource"
      (is (map? (put-resource (gen-customer-url existing-id)
                              {:name "Foo"
                               :address {:line1 "Foo"
                                         :city "Bar"}}))))))

(deftest test-get
  (let [customer-id (:id (post-resource (gen-customers-url :test)
                                        {:email "test@example.com"}))]
    (testing "get-resource"
      (is (map? (get-resource (gen-customer-url customer-id)))))))

(deftest test-requirements
  (let [id (:id (post-resource (gen-customers-url :test)
                               {:email "test@example.com"}))]
    (testing "get-requirements"
      (is (not (nil? (get-requirements id)))))
    (testing "parse-requirements")))

(deftest test-schemas
  (testing "Customer works"
    (is (= {:email "devnull@vaulted.io"}
           (s/validate Customer {:email "devnull@vaulted.io"})))
    (is (= {:email "devnull@vaulted.io" :name "Foobar"}
           (s/validate Customer {:email "devnull@vaulted.io" :name "Foobar"}))))
  (testing "Customer doesn't work"
    (is (thrown? Exception (s/validate Customer {:name "Foobar"})))))

;;  (run-tests)

