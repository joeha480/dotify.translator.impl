[![Build Status](https://travis-ci.com/brailleapps/dotify.translator.impl.svg?branch=master)](https://travis-ci.com/brailleapps/dotify.translator.impl)
[![Type](https://img.shields.io/badge/type-provider_bundle-blue.svg)](https://github.com/brailleapps/wiki/wiki/Types)
[![License: LGPL v2.1](https://img.shields.io/badge/License-LGPL%20v2%2E1%20%28or%20later%29-blue.svg)](https://www.gnu.org/licenses/lgpl-2.1)

# Introduction #
dotify.translator.impl contains an implementation of the translator interfaces of [dotify.api](https://github.com/joeha480/dotify/tree/master/dotify.api). If you want to use it, you can get it [here](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22dotify.translator.impl%22).

## Techniques
Java, Java SPI, OSGi

## Limitations
Currently, braille translation is implemented for Swedish only.

## Using ##
Download the [latest release](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.daisy.dotify%22%20%20a%3A%22dotify.translator.impl%22) from maven central and add it to your runtime environment.

Access the implementations via the following APIs in [dotify.api](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.daisy.dotify%22%20%20a%3A%22dotify.api%22):
- `BrailleFilterFactoryMaker`
- `BrailleTranslatorFactoryMaker`
- `MarkerProcessorFactoryMaker`
- `TextBorderFactoryMaker`

 _or_ in an OSGi environment use:
- `BrailleFilterFactoryMakerService`
- `BrailleTranslatorFactoryMakerService`
- `MarkerProcessorFactoryMakerService`
- `TextBorderFactoryMakerService`

## Building ##
Build with `gradlew build` (Windows) or `./gradlew build` (Mac/Linux)

## Testing ##

Tests are run with `gradlew test` (Windows) or `./gradlew test` (Mac/Linux)

## Requirements & Compatibility ##
- Requires Java 8
- Compatible with SPI and OSGi

## More information ##
See the [common wiki](https://github.com/brailleapps/wiki/wiki) for more information.
