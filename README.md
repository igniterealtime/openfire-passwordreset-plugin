# Openfire password reset plugin

This plugin provides the ability for users to reset their own passwords by sending a unique link to
the email address associated with their account.

## Building the plugin
```shell script
gradlew build
```

## Installing the plugin manually
1. Uninstall any existing version of the plugin from Openfire
2. Rename the newly built file `build/libs/PasswordReset-${version}-openfire-plugin-assembly.jar` to 
`passwordreset.jar`
3. Copy `passwordreset.jar` to the plugins folder of Openfire

## Installing the plugin from Gradle
Simply run 
```shell script
gradlew deploy
```
This task will perform the following steps to install the plugin:
1. Assemble the plugin - note that this will not perform any checks on the code so you may want to
add `check` as an additional task at the command line.
2. Delete any existing plugin in the Openfire plugins directory.
3. Wait for the old plugin to be uninstalled from Openfire.
4. Copy the newly built plugin in to the plugins directory.
5. Wait for the new plugin to be installed to Openfire.

Notes;
1. The task requires the `OPENFIRE_HOME` environment variable to be set to the location of the 
Openfire installation.
2. It goes without saying that Openfire needs to be running in order for step 3 above to work!   
3. [An issue with Jetty](https://github.com/eclipse/jetty.project/issues/1425) may mean that 
Openfire is unable to uninstall the existing plugin if you are running Java 9 or higher.
4. By default Openfire will only check the plugins folder for changes every 20 seconds. You can 
speed up steps 3 and 5 above by setting the System Property `plugins.loading.monitor.interval` to a 
smaller value, e.g. a value of `1` will tell Openfire to check the plugins folder for changes every 
second.