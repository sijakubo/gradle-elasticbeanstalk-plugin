package de.jansauer.elasticbeanstalk

import org.gradle.api.Project
import org.gradle.api.provider.Property

class ElasticBeanstalkExtension {

  final Property<String> applicationName
  final Property<Integer> versionToPreserve

  ElasticBeanstalkExtension(Project project) {
    applicationName = project.objects.property(String)
    versionToPreserve = project.objects.property(Integer)
  }
}
