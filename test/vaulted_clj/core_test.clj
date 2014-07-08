(ns vaulted-clj.core-test
  (:require [clojure.test :refer :all]
            [vaulted-clj.core :refer :all]
            [vaulted-clj.util :as util]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [schema.core :as s]))

;; WARNING: Tests assumes VAULTED-API-KEY is in the environment.

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

  ;; PENDING: better error messages
  (testing "request cannot be fulfilled due to bad syntax"
    (is (thrown? Exception
                 (post-resource (gen-customers-url :test) {:name "Foobar"})))

    ;; PENDING: better error messages
    (is (thrown? Exception
                 (get-resource (gen-customer-url "faulty" :test)))))

  (testing "authentication is possible but has failed"
    (is (thrown-with-msg?
         Exception #"access denied"
         (with-token ""
           (post-resource (gen-customers-url :test)
                          {:email "test@example.com"}))))))

(deftest test-post
  (let [customer (post-resource (gen-customers-url :test) {:email "test@example.com"})
        debit {:number (str (rand-int 1000000))
               :items [{:amount 5000
                        :vat_percent 12.5
                        :description "Flicknex Streaming."
                        :period {:start "1396232247" :end "1398824247"}
                        :quantity 1}]
               :description ""}]
    
    (testing "post-resource"
      (is (map? (post-resource (gen-customers-url :test) {:email "test@example.com"}))))

    (testing "post-resource with post-debit")
    (is (post-resource (gen-debit-url (:id customer)) debit))))

;; Testing post-debit not working.
;;
;; (def foocust (post-resource (gen-customers-url :test) {:email "test@example.com"}))
;; (def foodeb {:number (str (rand-int 1000000))
;;                :items [{:amount 5000
;;                         :vat_percent 12.5
;;                         :description "Flicknex Streaming."
;;                         :period {:start "1396232247" :end "1398824247"}
;;                         :quantity 1}]
;;              :description ""})
;; (def foodeburl (gen-debit-url (:id foocust)))
;; (def realdebit {:number "213123"
;;                 :items [{:amount 500
;;                          :description "Foo"}]})

;; (def realopts (assoc (make-options-map) :form-params realdebit))
;; (handle-response
;;  client/get
;;  "https://test.vaulted.com/v0/customers/user_36518ebc-3166-4c35-b7a1-e7234acb3426"
;;  {:basic-auth *vaulted-key*
;;   :debug true
;;   :debug-body true
;;   :socket-timeout 3000
;;   :conn-timeout 3000
;;   :content-type :json
;;   :form-params realdebit
;;   :as :json
;;   })

(deftest test-put
  (let [existing-id (:id (post-resource (gen-customers-url :test)
                                        {:email "existing@example.com"}))]
    (testing "put-resource"
      (is (map? (put-resource (gen-customer-url existing-id)
                              {:name "Foo"
                               :address {:line1 "Foo"
                                         :city "Bar"}}))))))


(deftest test-get
  (let [customer-id (:id (post-resource (gen-customers-url :test) {:email "test@example.com"}))]

    (testing "get-resource"
      (is (map? (get-resource (gen-customer-url customer-id)))))

    ))

(deftest test-requirements
  (let [id (:id (post-resource (gen-customers-url :test) {:email "test@example.com"}))]

    (testing "get-requirements"
      (is (not (nil? (get-requirements id)))))

    (testing "parse-requirements")
    ))

(deftest test-schemas

  (testing "Customer works"
    (is (= {:email "devnull@vaulted.io"}
           (s/validate Customer {:email "devnull@vaulted.io"})))
    (is (= {:email "devnull@vaulted.io" :name "Foobar"}
           (s/validate Customer {:email "devnull@vaulted.io" :name "Foobar"}))))

  (testing "Customer doesn't work")

  ;; TODO: test exception message
  ;; Value does not match schema: {:email missing-required-key}
  (is (thrown? Exception
               (s/validate Customer {:name "Foobar"})))

  (is (thrown? Exception
               (s/validate Customer
                           {:email "devnull@vaulted.io" :lumberjack "Foobar"})))  
  )

(comment
  (run-tests)

  ;; defunc test, and possibly result
  (is (= (util/strings->fields
          (get-requirements "user_8232c84a-8f3f-421a-979e-3300400d30ab"))
         ({:address (:country_code :postal_code :city :line)} :name)))
  )

