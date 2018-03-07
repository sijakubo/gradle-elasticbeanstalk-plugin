package de.jansauer.elasticbeanstalk

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder
import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationVersionRequest
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class CleanupVersionsTask extends DefaultTask {

    @Input
    final Property<String> applicationName = project.objects.property(String)

    @Input
    final Property<Integer> versionToPreserve = project.objects.property(Integer)

    CleanupVersionsTask() {
        setDescription("Cleanup unused ElasticBeanstalk Application Versions.")
        setGroup("aws")
    }

    @TaskAction
    def cleanupVersions() {
        logger.info("Cleaning up elastic beanstalk versions for application '{}'", applicationName.get())
        def client = AWSElasticBeanstalkClientBuilder.standard()
                .withRegion('eu-central-1')
                .build()

        client.describeApplicationVersions(new DescribeApplicationVersionsRequest().withApplicationName(applicationName.get()))
                .getApplicationVersions()
                .findAll({ it.versionLabel ==~ /\d+\.\d+\.\d+-\d+-g.*/ })
                .drop(versionToPreserve.get())
                .each {
            client.deleteApplicationVersion(new DeleteApplicationVersionRequest()
                    .withApplicationName(applicationName.get())
                    .withVersionLabel(it.versionLabel)
                    .withDeleteSourceBundle(true))
            logger.debug("Beanstalk application version with label '${it.versionLabel}' was deleted.")
        }
    }
}
