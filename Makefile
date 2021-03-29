.PHONY: 

clean:
	-rm -rf resources/public/js/*	

watch-ui:
	npx shadow-cljs watch app

watch-css:
	clj -Sdeps '{:deps {lambdaisland/garden-watcher {:mvn/version "0.3.5"}}}' -e "(require '[garden-watcher.core :as gw]) (require '[com.stuartsierra.component :as component]) (component/start (gw/new-garden-watcher '[styles.main]))"

release-ui: clean
	npx shadow-cljs release app
