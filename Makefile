.EXPORT_ALL_VARIABLES:
.PHONY: test deploy

VERSION = $(shell cat VERSION)
DATE = $(shell date)

IMG = niquola/dojo

repl:
	source .env && clj -A:test:nrepl -R:test:nrepl -e "(-main)" -r

up:
	source .env && docker-compose up -d
stop:
	source .env && docker-compose stop

down:
	source .env && docker-compose down

jar:
	cd ui && make prod && cd .. && rm -rf target && clj -A:build

docker:
	docker build -t ${IMG} .

pub:
	docker push ${IMG}

deploy:
	cd deploy && cat srv.tpl.yaml | ./envtpl.mac 

all: jar docker pub deploy
	echo "Done"

test:
	source .env && clj -A:test:runner

