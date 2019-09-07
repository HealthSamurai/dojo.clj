repl:
	source ../.env && clj -A:dev -R:dev -e "(repl)" -r

clean:
	rm -rf target/ .cpcache/

prod:
	clj -m build
