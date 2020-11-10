# Rundeck Datadog Nodes Plugin
![](https://github.com/wayfair-incubator/rundeck-datadog-nodes-plugin/workflows/CI/badge.svg?branch=main)

This is a [Resource Model Source](https://docs.rundeck.com/docs/administration/projects/resource-model-sources/) plugin 
for Rundeck 3.0+ that provides Datadog hosts as nodes for use within Rundeck.

This plugin utilizes the Datadog [hosts endpoint](https://docs.datadoghq.com/api/v1/hosts/#get-all-hosts-for-your-organization)
to retrieve hosts that match a configured filter string. Filter strings can be tested using 
[Datadog's Infrastructure List](https://app.datadoghq.com/infrastructure) page. 

At this time, the hosts endpoint returns 1000 hosts max and takes a fairly long time when the host count is high 
(tested on 30,000+ hosts). In order to work around this, the calls are parallelized if more than 1000 hosts are to be returned.

Datadog tags can be configured as Rundeck node tags per project or at the framework level. All Datadog tags are not
included by default as it can lock up the Rundeck UI when there are hundreds of tags per node.

## Installation
Download from the [releases page](https://github.com/wayfair-incubator/rundeck-datadog-nodes-plugin/releases).

Alternatively, clone the repository, run `./gradlew build`, 
and obtain the resulting JAR in `build/libs/rundeck-datadog-nodes-plugin-X.X.X.jar`

Copy the plugin JAR to the Rundeck servers libext directory to complete the [installation](https://docs.rundeck.com/docs/administration/configuration/plugins/installing.html).

## Configure
You can configure the plugin properties via the three following methods:
* Rundeck UI
* Project properties file  
  * `project.plugin.ResourceModelSource.datadog-nodes-plugin.<property_name>=<value>`
* Framework properties file
  * `framework.plugin.ResourceModelSource.datadog-nodes-plugin.<property_name>=<value>`
  
See the Rundeck [Node Source Configuration](https://docs.rundeck.com/docs/administration/configuration/plugins/configuring.html#resource-model-sources) for more information.

### Configuration Properties
![](/images/plugin_configuration.png)
<br/>

**Datadog Api Key**

The api key for authenticating with Datadog - [Datadog Authentication](https://docs.datadoghq.com/account_management/api-app-keys/)

**Datadog App Key**

The app key for authenticating with Datadog - [Datadog Authentication](https://docs.datadoghq.com/account_management/api-app-keys/)

**Filter**

[Optional] A string used to filter hosts from Datadog. For example, `env:production host:test datacenter:mars` will return all production hosts
with test in their name in the mars datacenter. It's a good idea to check the filter string
on the [Datadog Infrastructure List](https://app.datadoghq.com/infrastructure) page to verify the host list you are targeting.
Datadog only supports "AND" filters at this time so only hosts with that exact combination of attributes will be returned.
If empty, all hosts will be returned.

**Datadog Tag File Path**

[Optional] A path to a file that contains a newline separated list of Datadog tags to add to Rundeck nodes. Nodes with hundreds of 
tags can freeze the Rundeck UI so this allows users to map only the tags they need.

**Datadog Tags**

[Optional] A comma separated list of Datadog tags to map to nodes. These will be appended to the list of tags if a tag file path is given.
Otherwise, they will be used on their own.

## Mapping Datadog Hosts to Rundeck Nodes
Rundeck nodes require certain attributes be set to ensure they can be utilized properly. Other attributes
are used to filter the nodes.  

The following attributes are set by this plugin:  
* `nodename`  - the unique node identifier
* `hostname`  - the hostname used for connecting to the node
* `osName`    - the operating system name (if available; if not the OS platform is used)
* `osFamily`  - the operating system family (unix, windows). Used by the node executor
* `osVersion` - operating system version (if available)
* `tags`      - Datadog tags
* `isMuted`   - whether or not the node is muted in Datadog
* `sources`   - the source of the node in Datadog (Ex. vSphere, Datadog Agent)

## License
See license information [here](LICENSE)

## Contributing
If you would like to contribute to this project, see the [Contributing](CONTRIBUTING.md) documentation for more information. Please ensure to follow our 
[Code of Conduct](CODE_OF_CONDUCT.md) at all times.
