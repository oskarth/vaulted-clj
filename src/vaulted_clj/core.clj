(ns vaulted-clj.core
  (:use [environ.core :only [env]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [vaulted-clj.util :as util]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [schema.core :as s]
            [schema.macros :as sm]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schemas

(def Address
  "A schema for an Address."
  {(s/optional-key :line1) s/Str ;; street
   (s/optional-key :line2) s/Str
   (s/optional-key :city) s/Str
   (s/optional-key :state) s/Str
   (s/optional-key :postal_code) s/Str
   (s/optional-key :country_code) s/Str}) ;; ISO 3166

(def BillableAddress
  "A schema for a Billable Address."
  {:line1 s/Str
   (s/optional-key :line2) s/Str
   :city s/Str
   (s/optional-key :state) s/Str
   :postal_code s/Str
   :country_code s/Str})

(def Customer
  "A schema for a Customer."
  {(s/optional-key :name) s/Str
   :email s/Str
   (s/optional-key :phone) s/Str
   (s/optional-key :address) Address
   (s/optional-key :vat_number) s/Str})

(def BillableCustomer
  "A schema for a Customer with all the fields required to be billable."
  {:email s/Str
   :name s/Str
   (s/optional-key :phone) s/Str
   (s/optional-key :address) Address
   (s/optional-key :vat_number) s/Str})

(def Period
  "A schema for a Period."
  {:start s/Str
   :end s/Str})

(def Item
  "A schema for an Item."
  {:amount s/Num
   :description s/Str
   (s/optional-key :vat_percent) double
   (s/optional-key :reverse_charge) boolean
   (s/optional-key :quantity) s/Num
   (s/optional-key :period) Period})

(def Debit
  "A schema for a Debit."
  {:number s/Str ;; document number
   :items [Item]
   (s/optional-key :description) s/Str
   (s/optional-key :instrument) s/Str}) ;; "invoice", "direct_debit", or "sofort"

(def Email
  "Schema for email"
  (s/pred #(re-matches util/+email-regex+ %)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc

(def ^:dynamic *vaulted-key* (env :vaulted-api-key))

(defn gen-base-url
  "Generates base-url depending on live/test mode in environment, and mode.
   Uses live mode only if mode is nil and env is live, or if mode is live."
  [& [mode]]
  (cond (and (= mode nil) (env :api-mode)) "https://api.vaulted.com/v0"
        (= mode :live) "https://api.vaulted.com/v0"
        :else "https://test.vaulted.com/v0"))

(defn gen-customers-url [& [mode]]
  (str (gen-base-url mode) "/customers"))

(defn gen-customer-url [id & [mode]]
  (str (gen-customers-url mode) "/" id))

(defn gen-debit-url [id & [mode]]
  (str (gen-customers-url mode) "/" id "/debits"))

(defn gen-requirements-url [id & [mode]]
  (str (gen-customers-url mode) "/" id "/requirements"))

(defn gen-customer-email-url [email & [mode]]
  (str (gen-customers-url mode) "?email=" email))

(defn make-options-map
  "Default request options map. A function because *vaulted-key* might change."
  []
  {:basic-auth *vaulted-key*
   :socket-timeout 15000
   :conn-timeout 15000
   :content-type :json
   :as :json})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Generic resource functions

(defn handle-response
  "Tries to make a request and throws custom errors based on status
  code. Returns the body of a response."
  [req-fn url options]
  (try+
   (:body (req-fn url options))
   (catch [:status 401] {:keys [body]}
     (throw+ {:status 401 :message "access denied"}))
   (catch [:status 404] {:keys [body]}
     (throw+ {:status 404 :message (get (json/parse-string body) "additional")}))
   (catch [:status 400] {:keys [body]}
     (throw+ {:status 400 :message (get (json/parse-string body) "additional")}))
   (catch [:status 500] {:keys [body]}
     (throw+ {:status 500 :message (get (json/parse-string body) "additional")}))))

(defn get-resource
  "GET resource at url."
  [url]
  (let [options (make-options-map)]
    (handle-response client/get url options)))

(defn post-resource
  "POST params map to resource url."
  [url params]
  (let [options (assoc (make-options-map) :form-params params)]
    (handle-response client/post url options)))

(defn put-resource
  "PUT params map to resource url."
  [url params]
  (let [options (assoc (make-options-map) :form-params params)]
    (handle-response client/put url options)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Customer

(sm/defn get-customer :- s/Str
  "GET a customer with their id. Returns a customer map."
  [id :- Customer & [mode :- s/Keyword]]
  (get-resource (gen-customer-url id mode)))

(sm/defn post-customer :- Customer
  "POSTs a customer. If successful, return a customer with more keys."
  [customer :- Customer & [mode :- s/Keyword]]
  (post-resource (gen-customers-url mode) customer))

(sm/defn get-requirements :- [s/Str]
  "Returns a vector of requirements that have to be filled. Use
  util/strings->fields to parse it into nested keywords."
  [id :- s/Str & [mode :- s/Keyword]]
  (:fields (get-resource (gen-requirements-url id mode))))

(sm/defn put-customer :- Customer
  "Updates a customer."
  [id :- s/Str customer :- Customer & [mode :- s/Keyword]]
  (put-resource (gen-customer-url id mode)
                (dissoc customer :email)))

(sm/defn get-customer-by-email :- Customer
  "Get a customer by email."
  [email :- Email & [mode :- s/Keyword]]
  (get-resource (gen-customer-email-url email mode)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Debit

(sm/defn post-debit :- Debit
  "Takes a customer id and posts a debit with a debit map."
  [id :- s/Str debit :- Debit & [mode :- s/Keyword]]
  (post-resource (gen-debit-url id mode) debit))

;; TODO get debit
;; TODO get debit by ref

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Refund

;; TODO post refund
;; TODO get refund
;; TODO get refund by ref

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Credit

;; TODO post credit
;; TODO get credit
;; TODO get credit by ref

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Macros

(defmacro with-token
  "Binds the specificed token to the vaulted-key variable and executes the body."
  [token & body]
  `(binding [*vaulted-key* ~token] ~@body))
