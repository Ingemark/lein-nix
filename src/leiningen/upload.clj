(ns leiningen.upload
  (:require [lein-nix.core :refer [abort sh!]]
            (leiningen [deploy :refer [repo-for]])
            [leiningen.core.main :as main]
            (clojure.java [shell :as sh] [io :as io])
            [clojure.string :as s]
            [cemerick.pomegranate.aether :as aether])
  (:import cemerick.pomegranate.aether.PomegranateWagonProvider
           org.apache.maven.wagon.Wagon
           org.apache.maven.wagon.repository.Repository
           org.apache.maven.wagon.authentication.AuthenticationInfo))

(set! *warn-on-reflection* true)

#_
(defn aether-deploy [project file]
  (let [files {[(symbol (:group project) (:name project)) (:version project)
                :extension "tgz"]
               file}]
    (aether/deploy-artifacts
     :artifacts (keys files)
     :files files
     :transfer-listener :stdout
     :repository [(leiningen.deploy/repo-for project "releases")])))

(defn upload
  "Upload a file to a repository

Supports:
- repository URLs that work with the standard lein deploy mechanism
- SourceForge via a repository URL of the form forge://<project_name>"
  [project file reponame]
  (let [filename (if (map? file) (-> file vals first) file)
        f (io/file filename)
        repo (second (leiningen.deploy/repo-for project reponame))
        repo-obj (Repository. "" (:url repo))
        proto (.getProtocol repo-obj)
        wagon (.lookup (PomegranateWagonProvider.) proto)]
    (println "Upload" filename "==>" (:url repo))
    (cond
     wagon (doto wagon
             (.connect repo-obj (doto (AuthenticationInfo.)
                                  (.setUserName (:username repo))
                                  (.setPassword (:password repo))
                                  (.setPassphrase (:passphrase repo))))
             (.put f (.getName f)))
     (= proto "forge")
     (sh! "rsync" "-e" "ssh" (.getPath f)
          (format "%s@frs.sourceforge.net:/home/frs/project/%s/"
                  (:username repo) (.getHost repo-obj))))))
