(ns mxtodomvc.todo
  (:require
    [clojure.string :as str]
    [tiltontec.util.core :as util :refer [pln now map-to-json json-to-map uuidv4]]
    [tiltontec.cell.core
     :refer-macros [cF cFn] :refer [cI]]
    [tiltontec.model.core :as md :refer [make <mget mset!>]]))

(declare td-deleted td-completed make-todo)

(defn todo-list [seed-todos]
  (md/make ::todo-list
    :items-raw (cFn (for [td seed-todos]
                      (make-todo {:title td})))
    ;:items (cF (doall (remove td-deleted (<mget me :items-raw))))
    ;
    ;;; the TodoMVC challenge has a requirement that routes "go thru the
    ;;; the model". (Some of us just toggled the hidden attribute appropriately
    ;;; and avoided the DOM add/removal.) An exemplar they provided had the view
    ;;; examine the route and ask the model for different subsets using different
    ;;; functions for each subset. For fun we used dedicated cells:
    ;
    ;:items-completed (cF (doall (filter td-completed (<mget me :items))))
    ;:items-active (cF (doall (remove td-completed (<mget me :items))))
    ;
    ;;; two DIVs want to hide if there are no to-dos, so in the spirit
    ;;; of DRY we dedicate a cell to that semantic.
    ;:empty? (cF (empty? (<mget me :items)))
    ))

(defn make-todo
  "Make a matrix incarnation of a todo on initial entry"
  [islots]

  (let [net-slots (merge
                    {:id        (uuidv4)
                     ;; we wrap mutable slots as Cells...
                     :title     (cI (:title islots))
                     :completed (cI nil)
                     :deleted   (cI nil)})
        todo (apply md/make (flatten (into [] net-slots)))]
    todo))

;;; --------------------------------------------------------
;;; --- handy accessors to hide <mget etc ------------------
;;; look for a macro RSN to auto-generate these

(defn td-created [td]
  ;; created is not a Cell because it never changes, but we use the <mget API anyway
  ;; just in case that changes. (<mget can handle normal slots not wrapped in cells.)
  (<mget td :created))

(defn td-title [td]
  (<mget td :title))

(defn td-id [td]
  (<mget td :id))

(defn td-completed [td]
  (<mget td :completed))

(defn td-deleted [td]
  ;; created is not a Cell because it never changes, but we use the <mget API anyway
  ;; just in case that changes (eg, to implement un-delete)
  (<mget td :deleted))

;;; ---------------------------------------------
;;; --- dataflow triggering setters to hide mset!

(defn td-delete! [td]
  (assert td)
  (mset!> td :deleted (now)))

(defn td-toggle-completed! [td]
  (mset!> td :completed (when-not (td-completed td) (now))))
