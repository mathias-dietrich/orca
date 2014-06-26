/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.batch.workflow

import spock.lang.Specification
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.orca.api.JobStarter
import com.netflix.spinnaker.orca.test.batch.BatchTestConfiguration
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import static org.springframework.batch.repeat.RepeatStatus.FINISHED
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD

@ContextConfiguration(classes = [BatchTestConfiguration])
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class WorkflowConfigurationSpec extends Specification {

  @Autowired ApplicationContext applicationContext
  @Autowired JobBuilderFactory jobs
  @Autowired StepBuilderFactory steps
  @Autowired JobLauncher jobLauncher
  @Autowired JobRepository jobRepository

  def jobStarter = new JobStarter()
  def fooTasklet = Mock(Tasklet)
  def barTasklet = Mock(Tasklet)
  def bazTasklet = Mock(Tasklet)

  def setup() {
    applicationContext.autowireCapableBeanFactory.with {
      registerSingleton("mapper", new ObjectMapper())
      registerSingleton("fooWorkflowBuilder", new TestWorkflowBuilder(fooTasklet, steps))
      registerSingleton("barWorkflowBuilder", new TestWorkflowBuilder(barTasklet, steps))
      registerSingleton("bazWorkflowBuilder", new TestWorkflowBuilder(bazTasklet, steps))
    }

    applicationContext.autowireCapableBeanFactory.autowireBean(jobStarter)
  }

  def "a single workflow step is constructed from mayo's json config"() {
    given:
    def mapper = new ObjectMapper()
    def config = mapper.writeValueAsString([
      [type: "foo"]
    ])

    when:
    jobStarter.start(config)

    then:
    1 * fooTasklet.execute(*_) >> FINISHED
  }

  def "multiple workflow steps are constructed from mayo's json config"() {
    given:
    def mapper = new ObjectMapper()
    def config = mapper.writeValueAsString([
      [type: "foo"],
      [type: "bar"],
      [type: "baz"]
    ])

    when:
    jobStarter.start(config)

    then:
    1 * fooTasklet.execute(*_) >> FINISHED

    then:
    1 * barTasklet.execute(*_) >> FINISHED

    then:
    1 * bazTasklet.execute(*_) >> FINISHED
  }
}
