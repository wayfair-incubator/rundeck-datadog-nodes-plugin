package com.wayfair.rundeck.plugin.resources.datadog

import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.wayfair.rundeck.plugin.resources.datadog.datadog.DatadogHost
import com.wayfair.rundeck.plugin.resources.datadog.datadog.DatadogMetaData
import spock.lang.Specification

class HostToNodeMapperSpec extends Specification {
    def "datadog windows hosts returned"() {
        given:

        Set<DatadogHost> hostList = [mkDatadogHost(1), mkDatadogHost(3), mkDatadogHost(5)]

        NodeSetImpl nodeSet = HostToNodeMapper.mapHosts(hostList, new HashSet<String>())

        expect:
        nodeSet.every { node ->
            node.getNodename().contains('example.com')
            node.getOsFamily() == 'windows'
            node.getOsName() == 'Windows Server 2222 Datacenter'
            node.getOsVersion() == '22.2 Build 99999'
        }
    }

    def "datadog linux hosts returned"() {
        given:
        Set<DatadogHost> hostList = [mkDatadogHost(0), mkDatadogHost(2), mkDatadogHost(4)]

        NodeSetImpl nodeSet = HostToNodeMapper.mapHosts(hostList, new HashSet<String>())

        expect:
        nodeSet.every { node ->
            node.getNodename().contains('example.com') &&
                    node.getOsFamily() == 'unix' &&
                    node.getOsName() == 'Centos' &&
                    node.getOsVersion() == '22.22.2222'
        }
    }

    def "empty datadog host list returned"() {
        given:

        Set<DatadogHost> hostList = []

        NodeSetImpl nodeSet = HostToNodeMapper.mapHosts(hostList, new HashSet<String>())

        expect:
        nodeSet.getNodes().size() == 0
    }

    def "user configured tag added to configuration"() {
        given:
        Set<DatadogHost> hostList = [mkDatadogHost(0)]
        Set<String> tags = new HashSet<>()
        tags.add('operatingsystem')

        NodeSetImpl nodeSet = HostToNodeMapper.mapHosts(hostList, tags)

        expect:
        nodeSet.getNode("host0.example.com").getTags().contains("operatingsystem:centos")
    }

    def "datadog host returned without metadata"() {
        given:
        Set<DatadogHost> hostList = [mkDatadogHost(0, false), mkDatadogHost(1, false)]

        NodeSetImpl nodeSet = HostToNodeMapper.mapHosts(hostList, new HashSet<String>())

        expect:
        nodeSet.every { node ->
            node.getOsFamily() == 'unix' || node.getOsFamily() == 'windows'
        }
    }

    def "datadog node returned without OS infomation discarded"() {
        given:
        Set<DatadogHost> hostList = [mkDatadogHost(0, false)]
        hostList[0].setTags(new HashMap<String, List<String>>())

        NodeSetImpl nodeSet = HostToNodeMapper.mapHosts(hostList, new HashSet<String>())

        expect:
        nodeSet.getNodes().size() == 0
    }

    private static DatadogHost mkDatadogHost(id, withMetaData = true) {
        DatadogHost datadogHost = new DatadogHost()
        datadogHost.setHostName("host${id}.example.com")
        datadogHost.setId(id)
        datadogHost.setMuted(true)
        datadogHost.setSources(new ArrayList<String>())
        datadogHost.setTags(Collections.singletonMap(
                'Datadog Agent',
                Collections.singletonList(id % 2 == 0 ? 'operatingsystem:centos' : 'operatingsystem:windows')
        ))

        if (withMetaData) {
            datadogHost.setMetaData(mkDataDogMetaData(id))
        }

        return datadogHost
    }

    private static DatadogMetaData mkDataDogMetaData(id) {
        DatadogMetaData datadogMetaData = new DatadogMetaData()
        datadogMetaData.setOperatingSystemPlatform(id % 2 == 0 ? "linux" : "windows")
        if (datadogMetaData.getOperatingSystemPlatform() == "windows") {
            datadogMetaData.setWindowsVersionList(
                    new ArrayList<>(
                            Arrays.asList("Windows Server 2222 Datacenter", "22.2 Build 99999", "")
                    )
            )
            datadogMetaData.setUnixVersionList(new ArrayList<>(Arrays.asList("", "", "")))
        } else {
            datadogMetaData.setUnixVersionList(new ArrayList<>(Arrays.asList("Centos", "22.22.2222", "")))
            datadogMetaData.setWindowsVersionList(new ArrayList<>(Arrays.asList("", "", "")))
        }

        return datadogMetaData
    }
}