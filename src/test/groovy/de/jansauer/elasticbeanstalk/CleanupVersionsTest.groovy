package de.jansauer.elasticbeanstalk

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder
import com.amazonaws.services.elasticbeanstalk.model.*
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.gradle.internal.impldep.org.junit.Rule
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class CleanupVersionsTest extends Specification {

  @Shared
  String applicationName

  @Shared
  AWSElasticBeanstalk client

  @Shared
  AmazonS3 s3Client

  @Shared
  String s3BucketName

  @Rule
  final TemporaryFolder testProjectDir = new TemporaryFolder()

  File buildFile

  def setupSpec() {
    applicationName = 'Gradle Plugin Test'
    s3BucketName = 'elasticbeanstalk-eu-central-1-640667059075'
    s3Client = AmazonS3ClientBuilder.standard().withRegion('eu-central-1').build()
    client = AWSElasticBeanstalkClientBuilder.standard()
        .withRegion('eu-central-1')
        .build()
    client.createApplication(new CreateApplicationRequest()
        .withApplicationName(applicationName))
    createApplicationVersion('0.0.0')
    createApplicationVersion('1.0.0-2-g4b03a14')
    createApplicationVersion('1.0.2-20-g4b03a14')
    createApplicationVersion('1.5.20-21-g4b03a14')
    createApplicationVersion('20.10.1-5-g4b03a14')
    createApplicationVersion('1.0.0')
  }

  def setup() {
    testProjectDir.create()
    buildFile = testProjectDir.newFile('build.gradle')
  }

  def "should cleanup old application versions"() {
    given:
    buildFile << """
        plugins {
          id 'de.jansauer.elasticbeanstalk'
        }
        
        elasticBeanstalk {
          applicationName = 'Gradle Plugin Test'
          versionToPreserve = 2
        }
    """

    when:
    def result = GradleRunner.create()
        .withGradleVersion(gradleVersion)
        .withProjectDir(testProjectDir.root)
        .withArguments('cleanupVersions')
        .withDebug(true)
        .withPluginClasspath()
        .build()
    print result.output

    then:
    result.task(':cleanupVersions').outcome == SUCCESS

    when:
    def remainingVersions = client.describeApplicationVersions(new DescribeApplicationVersionsRequest()
        .withApplicationName(applicationName))
        .getApplicationVersions()
    then:
    remainingVersions.size() == 4
    remainingVersions.collectNested({
      it.versionLabel
    }) == ['1.0.0', '20.10.1-5-g4b03a14', '1.5.20-21-g4b03a14', '0.0.0']

    where:
    gradleVersion << ['4.5', '4.6', '4.7', '4.8', '4.8.1', '4.9', '4.10', '4.10.1', '4.10.2', '4.10.3']
  }

  def cleanupSpec() {
    client.deleteApplication(new DeleteApplicationRequest().withApplicationName(applicationName))
  }

  def createApplicationVersion(String version) {
    s3Client.putObject(s3BucketName, version + '.jar', version as String)
    client.createApplicationVersion(new CreateApplicationVersionRequest()
        .withApplicationName(applicationName)
        .withVersionLabel(version)
        .withDescription('Test application version ' + version)
        .withSourceBundle(new S3Location()
            .withS3Bucket(s3BucketName)
            .withS3Key(version + '.jar'))
        .withProcess(false))
  }
}
