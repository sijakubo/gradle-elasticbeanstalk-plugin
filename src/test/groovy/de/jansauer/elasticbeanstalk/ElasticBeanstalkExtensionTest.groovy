package de.jansauer.elasticbeanstalk

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ElasticBeanstalkExtensionTest extends Specification {

  def "should keep last 8 versions on default"() {
    when:
    Project project = ProjectBuilder.builder().withName("hello-world").build()
    project.pluginManager.apply ElasticBeanstalkPlugin
    def test = project.extensions.elasticBeanstalk.versionToPreserve

    then:
    project.extensions.elasticBeanstalk instanceof ElasticBeanstalkExtension
    project.extensions.elasticBeanstalk.versionToPreserve.get() == 8
  }
}
