# Kabrioletas [![ci-badge][]][ci] [![gitter-badge][]][gitter]

[ci]:                    https://github.com/2m/citywasp-api/actions
[ci-badge]:              https://github.com/2m/citywasp-api/workflows/ci/badge.svg
[gitter]:                https://gitter.im/2m/kabrioletas
[gitter-badge]:          https://badges.gitter.im/2m/kabrioletas.svg
[kabrioletas]:           https://twitter.com/kabrioletas
[kabrioletas-badge]:     https://img.shields.io/twitter/follow/kabrioletas.svg?style=social&label=Follow
[fijatas]:               https://twitter.com/fijatas
[fijatas-badge]:         https://img.shields.io/twitter/follow/fijatas.svg?style=social&label=Follow
[tautovezis]:            https://twitter.com/tautovezis
[tautovezis-badge]:      https://img.shields.io/twitter/follow/tautovezis.svg?style=social&label=Follow
[pirmukas]:              https://twitter.com/pirmukas
[pirmukas-badge]:        https://img.shields.io/twitter/follow/pirmukas.svg?style=social&label=Follow

Kabrioletas is a twitter bot that lets you know when you can get that sweet roofless ride.

The following twitter accounts have been or still are running this bot:

* [![kabrioletas-badge][]][kabrioletas] [@kabrioletas](https://twitter.com/kabrioletas)
* [![fijatas-badge][]][fijatas] [@fijatas](https://twitter.com/fijatas)
* [![tautovezis-badge][]][tautovezis] [@tautovezis](https://twitter.com/tautovezis)
* [![pirmukas-badge][]][pirmukas] [@pirmukas](https://twitter.com/pirmukas)

## Running

### Coursier

Copy [`application.conf`](src/main/resources/application.conf) to `kabrioletas.conf` and fill in all of the required credentials. Then run:

```bash
coursier launch \
  --repository bintray:2m/maven \
  lt.dvim.citywasp:kabrioletas_2.12:1.7 \
  -- \
  -J-Dconfig.file=kabrioletas.conf
```

### Docker

Take a look at https://github.com/2m/kabrioletas-docker-amd64
