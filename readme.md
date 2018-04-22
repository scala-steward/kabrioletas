# Kabrioletas [![bintray-badge][]][bintray] [![travis-badge][]][travis] [![gitter-badge][]][gitter]

[bintray]:               https://bintray.com/2m/maven/kabrioletas
[bintray-badge]:         https://img.shields.io/bintray/v/2m/maven/kabrioletas.svg?label=kabrioletas
[travis]:                https://travis-ci.org/2m/kabrioletas
[travis-badge]:          https://travis-ci.org/2m/kabrioletas.svg?branch=master
[gitter]:                https://gitter.im/2m/kabrioletas
[gitter-badge]:          https://badges.gitter.im/2m/kabrioletas.svg
[kabrioletas]:           https://twitter.com/kabrioletas
[kabrioletas-badge]:     https://img.shields.io/twitter/follow/kabrioletas.svg?style=social&label=Follow
[fijatas]:               https://twitter.com/fijatas
[fijatas-badge]:         https://img.shields.io/twitter/follow/fijatas.svg?style=social&label=Follow

Kabrioletas is a twitter bot that lets you know when you can get that sweet roofless ride.

There are currently two twitter accounts that are running this bot:

* [![kabrioletas-badge][]][kabrioletas] [@kabrioletas](twitter.com/kabrioletas)
* [![fijatas-badge][]][fijatas] [@fijatas](twitter.com/fijatas)

## Running

### Coursier

Copy [`application.conf`](src/main/resources/application.conf) to `kabrioletas.conf` and fill in all of the required credentials. Then run:

```bash
coursier launch \
  --repository bintray:2m/maven \
  --main lt.dvim.citywasp.kabrioletas.Kabrioletas \
  lt.dvim.citywasp:kabrioletas_2.12:1.4 \
  -- \
  -Dconfig.file=kabrioletas.conf
```

### Docker

Take a look at https://github.com/2m/kabrioletas-docker-amd64
