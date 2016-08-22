# Workaround for Collections Ordering bug (Issue #CQ-94530) in AEM 6.2

This is a work-around for AEM issue #CQ-94530. If Adobe publishes an official hotfix, we will link to it from here.

## Modules

The main parts of the template are:

* `core`: Java bundle containing all core functionality like OSGi services, listeners or schedulers, as well as component-related Java code such as servlets or request filters.
* `ui.apps`: contains the /apps (and /etc) parts of the project, i.e. JS & CSS clientlibs, components, templates, runmode specific configs as well as Hobbes-tests

## How to build

To build all the modules run in the project root directory the following command with Maven 3:

    mvn clean install

If you have a running AEM instance you can build and package the whole project and deploy into AEM with

    mvn clean install -PautoInstallPackage

Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallPackagePublish

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle
