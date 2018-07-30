(ns ^:figwheel-hooks mxtodomvc.core
  (:require
    [goog.dom :as dom]
    [clojure.string :as str]
    [tiltontec.cell.core :refer-macros [cFonce]]
    [tiltontec.model.core :refer [<mget] :as md]
    [mxweb.gen :refer-macros [h1 div]]
    [mxweb.html :refer [tag-dom-create]]))

(enable-console-print!)

(defn matrix-build []
  (md/make
    :mx-dom (cFonce (md/with-par me
                      [(div {}
                         (h1 {} "hello, Matrix"))]))))

(let [root (dom/getElement "tagroot")
      app-matrix (matrix-build)]
  (set! (.-innerHTML root) nil)
  (dom/appendChild root
    (tag-dom-create
      (<mget app-matrix :mx-dom))))