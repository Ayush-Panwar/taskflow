package com.taskflow.seed

import com.taskflow.project.Project
import com.taskflow.project.ProjectRepository
import com.taskflow.task.Task
import com.taskflow.task.TaskPriority
import com.taskflow.task.TaskPriority.HIGH
import com.taskflow.task.TaskPriority.LOW
import com.taskflow.task.TaskPriority.MEDIUM
import com.taskflow.task.TaskRepository
import com.taskflow.task.TaskStatus
import com.taskflow.task.TaskStatus.DONE
import com.taskflow.task.TaskStatus.IN_PROGRESS
import com.taskflow.task.TaskStatus.TODO
import com.taskflow.task.activity.TaskActivity
import com.taskflow.task.activity.TaskActivityRepository
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

/**
 * Creates a sample team so the app can be explored straight away. It only runs when
 * taskflow.seed.enabled is true, and does nothing if the demo user already exists.
 * Dates are relative to today, so the demo always includes overdue and upcoming tasks.
 */
@Component
@ConditionalOnProperty(prefix = "taskflow.seed", name = ["enabled"], havingValue = "true")
class DemoDataSeeder(
    private val users: UserRepository,
    private val projects: ProjectRepository,
    private val tasks: TaskRepository,
    private val activity: TaskActivityRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
    @Value("\${taskflow.seed.password}") private val password: String,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (users.existsByEmail(DEMO_EMAIL)) return
        require(password.isNotBlank()) { "taskflow.seed.password must be set when seeding is enabled" }

        val hash = passwordEncoder.encode(password)
        val demo = users.save(User("Demo User", DEMO_EMAIL, hash))
        val priya = users.save(User("Priya Sharma", "priya@taskflow.dev", hash))
        val sam = users.save(User("Sam Lee", "sam@taskflow.dev", hash))

        val website = projects.save(Project("Website Redesign", "New marketing site, launching this quarter.", demo))
        val mobile = projects.save(Project("Mobile App Launch", "Everything needed for the first public release.", demo))
        projects.save(Project("Team Offsite", "Nothing planned yet.", demo))

        val today = LocalDate.now(clock)

        task(website, "Fix checkout page on Safari", TODO, HIGH, today.minusDays(2), sam, "Payment button does nothing on Safari 17.")
        task(website, "Finalize homepage copy", TODO, HIGH, today.plusDays(2), demo, "Waiting on the last round of edits from marketing.")
        task(website, "Collect customer testimonials", TODO, MEDIUM, today.plusDays(6), priya)
        task(website, "Audit image sizes", TODO, LOW, null, null, "Anything over 200 KB should be compressed.")
        val navigation = task(website, "Build responsive navigation", IN_PROGRESS, HIGH, today.plusDays(1), demo)
        history(navigation, demo, TODO, IN_PROGRESS, Duration.ofHours(5))
        val blog = task(website, "Migrate blog to the new CMS", IN_PROGRESS, MEDIUM, today.minusDays(1), priya)
        history(blog, priya, TODO, IN_PROGRESS, Duration.ofDays(3))
        val palette = task(website, "Choose the color palette", DONE, MEDIUM, today.minusDays(5), sam)
        history(palette, sam, TODO, IN_PROGRESS, Duration.ofDays(8))
        history(palette, sam, IN_PROGRESS, DONE, Duration.ofDays(6))
        task(website, "Set up the staging site", DONE, LOW, null, demo)

        task(mobile, "Write App Store description", TODO, MEDIUM, today.plusDays(10), priya)
        task(mobile, "Prepare beta tester list", IN_PROGRESS, LOW, today.plusDays(4), sam)
        task(mobile, "Fix login crash on Android 12", TODO, HIGH, today, demo)

        log.info("Seeded demo data. Sign in as {}", DEMO_EMAIL)
    }

    private fun task(
        project: Project,
        title: String,
        status: TaskStatus,
        priority: TaskPriority,
        dueDate: LocalDate?,
        assignee: User?,
        description: String? = null,
    ): Task = tasks.save(Task(title, description, status, priority, dueDate, assignee, project))

    private fun history(task: Task, actor: User, from: TaskStatus, to: TaskStatus, ago: Duration) {
        activity.save(TaskActivity(task, actor, from, to, clock.instant().minus(ago)))
    }

    private companion object {
        const val DEMO_EMAIL = "demo@taskflow.dev"
    }
}
