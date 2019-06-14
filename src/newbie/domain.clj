(ns newbie.domain
  (:require
    [schema.core :as s]
    [clj-http.client :as client]
    [clojure.tools.logging :as log]
    [cheshire.core :as cheshire]
    [ring.util.codec :as encoder]
    ))

;; Domain
(def APP_ID "wx949359209ebdbc3f")
(def APP_SECRET "fbebd2283a5c9fa914eb595284e37426")
(def SCOPE "snsapi_userinfo")
(def STATE "state")
(def TIMEOUT 200)

(s/defschema Base
  {:result s/Str})

(s/defschema Response
  {:isSuccess s/Bool
   :errcode s/Int
   :errmsg s/Str
   :result {s/Keyword s/Any}})

;; Helper
(defn failResponse
  ([errcode errmsg]
   {:isSuccess false
    :errcode errcode
    :errmsg errmsg
    :result {}})
  ([response]
   (failResponse (:errcode response) (:errmsg response)))
  )

(defn succResponse
  [result]
  {:isSuccess true
   :errcode 0
   :errmsg ""
   :result result})

(defn get-weixin-accesstoken
  "Get weixin access token by code"
  [code]
  (try
    (let [result (client/get "https://api.weixin.qq.com/sns/oauth2/access_token"
                             {:query-params {:appid APP_ID
                                             :secret APP_SECRET
                                             :code code
                                             :grant_type "authorization_code"}
                              :timeout TIMEOUT})
          weixin-response (cheshire/parse-string (:body result) true)]
      (log/info "access token response: " weixin-response)
      (if (:errcode weixin-response)
        (do
          (log/error "get access token failed: " (:errcode weixin-response))
          (failResponse weixin-response))
        (succResponse weixin-response)))
    (catch Exception e
      (log/error ( "get access token error: " (.getMessage e))))))

(defn get-weixin-info
  [access_token openid]
  (try
    (let [result (client/get "https://api.weixin.qq.com/sns/userinfo"
                             {:query-params {:access_token access_token
                                             :openid openid
                                             :lang "zh_CN"}
                              :timeout TIMEOUT})
          weixin-response (cheshire/parse-string (:body result) true)]
      (log/info "userinfo response: " weixin-response)
      (if (:errcode weixin-response)
        (do
          (log/error "get open user info failed: " (:errcode weixin-response))
          (failResponse weixin-response))
        (succResponse weixin-response)))
    (catch Exception e
      (log/error ("get open user info error: " (.getMessage e))))))

;; Repository
(defn hello-world
  []
  {:result "Hello World"})

(defn weixin-info
  [code state]
  (let [accessTokenResponse (get-weixin-accesstoken code)
        success (:isSuccess accessTokenResponse)
        result (:result accessTokenResponse)]
    (if success
      (get-weixin-info (:access_token result) (:openid result))
      accessTokenResponse)))

(defn weixin-auth-info
  []
  (let [query-params {:appid APP_ID
                      :redirect_uri (encoder/url-encode "http://nicky.free.idcfengye.com/api/weixin-info")
                      :response_type "code"
                      :scope SCOPE
                      :state STATE}]
    (format "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=%s&scope=%s&state=%s#wechat_redirect"
            (:appid query-params)
            (:redirect_uri query-params)
            (:response_type query-params)
            (:scope query-params)
            (:state query-params))))



