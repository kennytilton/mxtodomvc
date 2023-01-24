(ns mxtodomvc.todo
  (:require
    [tiltontec.util.core :as util]
    [tiltontec.cell.core
     :refer-macros [cF cFn] :refer [cI]]
    [tiltontec.model.core :as md :refer [make mget mset! mswap!]]))

(declare td-deleted td-completed make-todo)

(defn todo-list [seed-todos]
  (md/make ::todo-list
    ;; the bulk of the to-do app does not care about deleted to-dos,
    ;; so we use a clumsy name "items-raw" for the true list of items
    ;; ever created, and save "items" for the ones actually used.
    ;;
    ;; we will skip peristence for a while and play a while with
    ;; to-dos (a) in memory (b) created only at start-up.
    :items-raw (cFn (for [td seed-todos]
                      (make-todo td)))
    ;; doall is needed so formula actually runs when asked, necessary
    ;; so read of any dependencies happens while dependent property
    ;; is bound as necessary. Look for this to get baked into the
    ;; Matrix internals.
    ;;
    :items (cF (doall (remove td-deleted (mget me :items-raw))))
    :items-completed (cF (doall (filter td-completed (mget me :items))))
    :items-active (cF (doall (remove td-completed (mget me :items))))

    :empty? (cF (empty? (mget me :items)))))

(defn make-todo
  "Make a matrix incarnation of a todo item"
  [title]
  ;; yes, we go further than the spec requires, but these
  ;; are the attributes a real-world CRUD app tracks and there
  ;; is little cost to including them.
  ;;
  ;; So we key off a UUID for when we get to persistence, record a
  ;; fixed creation time, use a timestamp to denote "completed", and
  ;; use another timestamp for logical deletion.
  ;;
  (md/make
    :id (util/uuidv4)
    :created (util/now)

    ;; we wrap mutable slots as Cells...
    :title (cI title)
    :completed (cI nil)
    :deleted (cI nil)))

;;; --------------------------------------------------------
;;; --- handy accessors to hide mget etc ------------------
;;; look for a macro RSN to auto-generate these

(defn td-created [td]
  ;; created is not a Cell because it never changes, but we use the mget API anyway
  ;; just in case that changes. (mget can handle normal slots not wrapped in cells.)
  (mget td :created))

(defn td-title [td]
  (mget td :title))

(defn td-id [td]
  (mget td :id))

(defn td-completed [td]
  (mget td :completed))

(defn td-deleted [td]
  ;; created is not a Cell because it never changes, but we use the mget API anyway
  ;; just in case that changes (eg, to implement un-delete)
  (mget td :deleted))

;;; ---------------------------------------------
;;; --- dataflow triggering setters to hide mset!

(defn td-delete! [td]
  (mset! td :deleted (util/now)))

(defn td-toggle-completed! [td]
  (mswap! td :completed
    #(when-not % (util/now))))
