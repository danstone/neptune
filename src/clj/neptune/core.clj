(ns neptune.core
  (:require [org.httpkit.server :as server]
            [clojure.java.io :as io]
            [ring.middleware.resource :as ring-resource]
            [eval-soup.core :as eval-soup]
            [clojure.tools.analyzer :as analyse]
            [clojure.tools.analyzer.jvm :as analyse-jvm])
  (:import (clojure.lang Namespace)
           (java.io StringWriter)))

(defmacro trace
  [& body]
  `(println "body:"))

(defmacro analyze
  [& body]
  `(analyse-jvm/analyze
    '(do ~@body)))

(defmacro defslider
  [sym & opts]
  `(do
     (when-not (resolve (quote ~sym))
       (def ~sym 0.0))
     [:neptune/slider (quote ~sym)]))

(def handler
  (->
    (fn [req]
      (if (= :post (:request-method req))
        {:headers {"Content-Type" "text/plain"}
         :status 200
         :body (try
                 (let [{:keys [form controls]} (read-string (slurp (:body req)))]
                   (pr-str
                     (let [[[results out] ns]
                           (eval-soup/eval-form-safely
                             (list
                               'binding
                               '[*out* (java.io.StringWriter.)]
                               '(require '[neptune.core :as nep])
                               (cons
                                 'do
                                 (doall
                                   (for [[sym val] controls]
                                     (do
                                       (prn sym val)
                                       (list 'alter-var-root
                                             (list 'resolve (list 'symbol (str sym)))
                                             (list 'constantly val))))))
                               [form
                                '(str *out*)])
                             (find-ns 'user))]
                       [(mapv (fn [x]
                                {:instarepl
                                 (binding [*print-level* 5
                                           *print-length* 5]
                                   (pr-str x))
                                 :control (if (and (vector? x))
                                            (case (first x)
                                              :neptune/slider x
                                              nil))
                                 :result
                                 {:out out
                                  :class (some-> (class x) .getName)
                                  :type (str (type x))
                                  :value
                                  (binding [*print-level* 5
                                            *print-length* 5]
                                    (pr-str x))}}) results)
                        (.getName ^Namespace ns)])))
                 (catch Throwable e
                   (prn e)
                   "error"))}
        {:headers {"Content-Type" "text/html"}
         :status 200
         :body (slurp (io/resource "neptune/index.html"))}))
    (ring-resource/wrap-resource "neptune")))

(defn start!
  []
  (server/run-server
    #'handler))

(comment
  (start!))