# Feedback
A Developer for z (IDz, formerly RDZ) plugin for making COBOL compiler messages easily accessible
## Features
TODO
# Getting started
## Building
1. Set up a [target platform](https://github.com/uricorin/feedback/wiki/Setting-up-a-target-platform)
2. Update the reference to the target platform in ./maven/cloud.corin.feedback/releng/cloud.corin.feedback.configuration/pom.xml 
3. Change directory to ./maven/cloud.corin.feedback
4. Run the following command "mvn clean verify" (requires maven)

Installable artifacts will be created in .maven/cloud.corin.feedback/releng/cloud.corin.feedback.update/target/

## Installing
1. Inside IDz, add the p2 update site produced in the previous step (or downloaded from [Releases](https://github.com/uricorin/feedback/releases)) to Available Software Sites
2. Under Install New Software, navigate to the new site and install the plugin 

# Docs
## Javadoc
https://uricorin.github.io/feedback/

## Eclipse plugins
https://www.vogella.com/tutorials/eclipseplatform.html


# License
This project is mostly licensed under the [MIT](LICENSE) license.  
The following class is the exception:
* [LineReader](maven/cloud.corin.feedback/bundles/cloud.corin.feedback.core/src/cloud/corin/common/rdz/LineReader.java)  
  * @Author Torleif Berger
  * @license http://creativecommons.org/licenses/by/3.0/
  * @see http://www.geekality.net/?p=1614
