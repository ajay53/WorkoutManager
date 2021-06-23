package com.goazi.workoutmanager.view.activity

import android.app.AlertDialog
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.goazi.workoutmanager.R
import com.goazi.workoutmanager.adapter.ExerciseListAdapter
import com.goazi.workoutmanager.databinding.CardSessionBinding
import com.goazi.workoutmanager.helper.Util
import com.goazi.workoutmanager.model.Exercise
import com.goazi.workoutmanager.model.Session
import com.goazi.workoutmanager.model.Workout
import com.goazi.workoutmanager.viewmodel.ExerciseViewModel
import com.goazi.workoutmanager.viewmodel.SessionViewModel
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.HashMap

class ExerciseActivity : AppCompatActivity(), ExerciseListAdapter.OnExerciseCLickListener,
    View.OnClickListener, Util.WorkOnClick, Util.RestOnClick {
    companion object {
        private const val TAG = "ExerciseActivity"
    }

    //widgets
    private lateinit var rvExercise: RecyclerView
    private lateinit var clParentAddPlay: ConstraintLayout
    private lateinit var clAddExercise: ConstraintLayout
    private lateinit var llTimer: LinearLayoutCompat
    private lateinit var tvPlay: TextView
    private lateinit var tvExerciseName: TextView
    private lateinit var tvSeconds: TextView
    private lateinit var imgStop: ImageView
    private lateinit var imgLock: ImageView
    private lateinit var imgReplay: ImageView
    private lateinit var imgPause: ImageView
    private lateinit var imgForward: ImageView

    //variables
    private lateinit var exercises: List<Exercise>
    private var exerciseCount: Int = 0
    private lateinit var exerciseViewModel: ExerciseViewModel
    private lateinit var sessionViewModel: SessionViewModel
    private var workoutId: Int = 0
    private lateinit var workout: Workout
    private var isAddExerciseClicked: Boolean = false
    private var isTimerRunning: Boolean = false
    private var seconds: Long = 10
    private var currExerciseName: String = ""
    private var currExerciseId: String = ""
    private var currExercisePosition: Int = 0
    private var currSessionPosition: Int = -1
    private lateinit var currentSession: Session
    private var isWork: Boolean = false
    private var isWorkoutRunning: Boolean = false
    private lateinit var timer: CountDownTimer
    private var dataMap: MutableMap<String?, MutableList<Session>> = HashMap()
    private var sessionMap: MutableMap<String?, LinearLayoutCompat> = HashMap()
    private var viewMap: MutableMap<String?, MutableList<View>> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_exercise)

        exerciseViewModel = ViewModelProvider(this).get(ExerciseViewModel::class.java)
        sessionViewModel = ViewModelProvider(this).get(SessionViewModel::class.java)
        workout = intent.extras!!.getParcelable("obj")!!
        workoutId = workout.id
        exerciseViewModel.searchById(workoutId)

        initViews()
    }

    private fun initViews() {
        clParentAddPlay = findViewById(R.id.cl_parent_add_play)

        clAddExercise = findViewById(R.id.cl_add_exercise)
        clAddExercise.setOnClickListener(this)

        tvPlay = findViewById(R.id.tv_play)
        tvPlay.setOnClickListener(this)

        imgStop = findViewById(R.id.img_stop)
        imgStop.setOnClickListener(this)

        imgLock = findViewById(R.id.img_lock)
        imgLock.setOnClickListener(this)

        imgReplay = findViewById(R.id.img_replay)
        imgReplay.setOnClickListener(this)

        imgPause = findViewById(R.id.img_pause)
        imgPause.setOnClickListener(this)

        imgForward = findViewById(R.id.img_forward)
        imgForward.setOnClickListener(this)

        tvSeconds = findViewById(R.id.tv_seconds)
        seconds = tvSeconds.text.toString().toLong() * 1000

        llTimer = findViewById(R.id.ll_timer)
        tvExerciseName = findViewById(R.id.tv_exercise_name)
        val tvWorkoutName = findViewById<TextView>(R.id.tv_workout_name)
        tvWorkoutName.text = workout.name

        rvExercise = findViewById(R.id.rv_exercise)
        var adapter =
            ExerciseListAdapter(applicationContext, exerciseViewModel.exercisesById.value, this)

        exerciseViewModel.exercisesById.observe(this, Observer { exercises ->
            if (exercises.size > 0) {
                currExerciseName = exercises[0].exerciseName!!
                currExerciseId = exercises[0].id
            }
            this.exercises = exercises
            if (exerciseCount == 0) {
                exerciseCount = exercises.size
                adapter =
                    ExerciseListAdapter(
                        applicationContext,
                        exerciseViewModel.exercisesById.value,
                        this
                    )
                rvExercise.adapter = adapter
                rvExercise.layoutManager = LinearLayoutManager(applicationContext)
                rvExercise.setHasFixedSize(false)
            } else {
                adapter.updateList(exercises)
            }
        })
    }

    private fun startTimer() {
        timer = object : CountDownTimer(seconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                seconds = millisUntilFinished
                updateCountDownText()
            }

            override fun onFinish() {
                isTimerRunning = false
                when {
                    currSessionPosition < dataMap[currExerciseId]!!.size - 1 || (currSessionPosition == dataMap[currExerciseId]!!.size - 1 && isWork) -> {
//                        isSessionClicked = false
                        isWork = !isWork
                        when {
                            isWork -> currSessionPosition++
                        }
                        tvExerciseName.text = currExerciseName
                        currentSession = dataMap[currExerciseId]!![currSessionPosition]
                        seconds =
                            if (isWork) currentSession.workTime!! else currentSession.restTime!!
                        isTimerRunning = true
                        startTimer()
                        animateView(seconds)
                    }
                    currExercisePosition < exercises.size - 1 -> {
//                        if (!isSessionClicked) {
//                            isSessionClicked = false
                        /*currExercisePosition++
                        currExerciseId = exercises[currExercisePosition].id
                        currExerciseName = exercises[currExercisePosition].exerciseName!!
                        currSessionPosition = 0
                        currentSession = dataMap[currExerciseId]!![currSessionPosition]*/
//                        } else {
                        /*currExercisePosition++
                        currExerciseId = exercises[currExercisePosition].id
                        currExerciseName = exercises[currExercisePosition].exerciseName!!
                        currSessionPosition = 0
                        currentSession = dataMap[currExerciseId]!![currSessionPosition]*/
//                        }
                        currExercisePosition++
                        currExerciseId = exercises[currExercisePosition].id
                        currExerciseName = exercises[currExercisePosition].exerciseName!!
                        currSessionPosition = 0
                        currentSession = dataMap[currExerciseId]!![currSessionPosition]
                        tvExerciseName.text = currExerciseName
                        isWork = !isWork
                        seconds =
                            if (isWork) currentSession.workTime!! else currentSession.restTime!!
                        isTimerRunning = true
                        startTimer()
                        animateView(seconds)
                    }
                    else -> {
                        stopTimer()
                    }
                }
            }
        }.start()
        isWorkoutRunning = true
        isTimerRunning = true
        imgPause.setImageResource(R.drawable.ic_pause)
    }

    //    var isSessionClicked: Boolean = false
    private fun pauseTimer() {
        imgPause.setImageResource(R.drawable.ic_play)
        timer.cancel()
        isTimerRunning = false
    }

    private fun stopTimer() {
        timer.cancel()
        isWorkoutRunning = false
        isTimerRunning = false
        llTimer.visibility = View.GONE
        clParentAddPlay.visibility = View.VISIBLE

        seconds = 5000
        currExerciseName = ""
        currExerciseId = ""
        currExercisePosition = 0
        currSessionPosition = -1

        dataMap.clear()
        sessionMap.clear()
        viewMap.clear()
        Util.showSnackBar(findViewById(R.id.activity_exercise), "Workout Stopped")
    }

    private fun animateView(seconds: Long) {
        val sessionList: MutableList<View> = viewMap[currExerciseId]!!
        val tv: TextView = if (isWork) {
            sessionList[currSessionPosition].findViewById(R.id.tv_work_time)
        } else {
            sessionList[currSessionPosition].findViewById(R.id.tv_rest_time)
        }
        tv.setTextColor(getColor(R.color.purple_700))
//        tv.setBackgroundColor(getColor(R.color.teal_700))
        /*val colorFrom = ContextCompat.getColor(applicationContext, R.color.green_light)
        val colorTo = ContextCompat.getColor(applicationContext, R.color.green_dark)
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = seconds // milliseconds

        colorAnimation.addUpdateListener { animator -> tv.setBackgroundColor(animator.animatedValue as Int) }
        colorAnimation.start()*/
    }

    private fun resetAnimation(isWork: Boolean) {
        val allSessionList: MutableList<MutableList<View>> = ArrayList(viewMap.values)

        for (i in 0 until currExercisePosition + 1) {
            val sessionList: MutableList<View> = allSessionList[i] // sessions in a exercise

            for (j in 0 until sessionList.size) {
                if (currSessionPosition == i && currSessionPosition == j) {
                    break
                }
                val workTv: TextView =
                    sessionList[j].findViewById(R.id.tv_work_time)
                workTv.setTextColor(getColor(R.color.purple_700))
                val restTv: TextView =
                    sessionList[j].findViewById(R.id.tv_rest_time)
                restTv.setTextColor(getColor(R.color.purple_700))
            }
        }

        var tempSessionPosition: Int = currSessionPosition
        if (!isWork) {
            tempSessionPosition++
        }


        for (i in currExercisePosition until allSessionList.size) {
            val sessionList: MutableList<View> = allSessionList[i] // sessions in a exercise

            for (j in tempSessionPosition until sessionList.size) {
                val workTv: TextView =
                    sessionList[tempSessionPosition].findViewById(R.id.tv_work_time)
                workTv.setTextColor(getColor(R.color.teal_200))
                val restTv: TextView =
                    sessionList[tempSessionPosition].findViewById(R.id.tv_rest_time)
                restTv.setTextColor(getColor(R.color.teal_200))
                tempSessionPosition++
            }
            tempSessionPosition = 0
        }
    }

    private fun setMap() {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        executor.execute(kotlinx.coroutines.Runnable {
            try {
//                dataMap = HashMap()
                for (exercise in exercises) {
                    val sessions: MutableList<Session> = sessionViewModel.getSessions(exercise.id)
                    dataMap[exercise.id] = sessions

                    val llSession: LinearLayoutCompat = sessionMap[exercise.id]!!
                    val childCount: Int = llSession.childCount
                    val sessionList: MutableList<View> = mutableListOf()
                    for (i in 0 until childCount) {
                        val ll: View = llSession.getChildAt(i)
                        sessionList.add(ll)
                    }
                    viewMap[exercise.id] = sessionList
                }
            } catch (e: Exception) {
                Log.d(TAG, "setMap: ")
            }
        })
    }

    private fun updateCountDownText() {
        val minutes = (seconds / 1000).toInt() / 60
        val seconds = (seconds / 1000).toInt() % 60
        val timeLeftFormatted: String =
            java.lang.String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        tvSeconds.text = timeLeftFormatted
    }

    private fun addExerciseDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val viewGroup: ViewGroup = findViewById(R.id.activity_exercise)
        val view: View = LayoutInflater.from(applicationContext)
            .inflate(R.layout.dialog_add_exercise, viewGroup, false)
        val edtExerciseName = view.findViewById<EditText>(R.id.edt_exercise_name)
        val btnSave = view.findViewById<Button>(R.id.btn_save)
        builder.setView(view)
        val alertDialog: AlertDialog = builder.create()
        btnSave.setOnClickListener {
            val uuid = UUID.randomUUID().toString()
            sessionViewModel.insert(Session(0, 10000, 5000, uuid))
            val unixTime: Long = System.currentTimeMillis() / 1000L
            exerciseViewModel.insert(
                Exercise(
                    uuid,
                    unixTime,
                    edtExerciseName.text.toString(),
                    workoutId
                )
            )
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    override fun onExerciseClick(position: Int) {
        Log.d(TAG, "onExerciseClick: ")
    }

    override fun onSessionAddClick(position: Int, llSessions: LinearLayoutCompat) {
        addSessionDialog(position, llSessions)
    }

    private fun addSessionDialog(position: Int, llSessions: LinearLayoutCompat) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val viewGroup: ViewGroup = findViewById(R.id.activity_exercise)
        val view: View = LayoutInflater.from(applicationContext)
            .inflate(R.layout.dialog_add_session, viewGroup, false)
        val edtWorkTime = view.findViewById<EditText>(R.id.edt_work_time)
        val edtRestTime = view.findViewById<EditText>(R.id.edt_rest_time)
        val btnSave = view.findViewById<Button>(R.id.btn_save)
        builder.setView(view)
        val alertDialog: AlertDialog = builder.create()
        btnSave.setOnClickListener {
            val session = Session(
                0,
                edtWorkTime.text.toString().toLong() * 1000,
                edtRestTime.text.toString().toLong() * 1000,
                exercises[position].id
            )
            //update UI
            val binding: CardSessionBinding =
                DataBindingUtil.inflate(layoutInflater, R.layout.card_session, null, false)
            binding.session = session

            val layoutParams: LinearLayoutCompat.LayoutParams =
                LinearLayoutCompat.LayoutParams(
                    LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                    LinearLayoutCompat.LayoutParams.MATCH_PARENT
                )
            layoutParams.setMargins(30, 0, 30, 20)
            binding.root.layoutParams = layoutParams
            binding.workClick = this
            binding.restClick = this
            llSessions.addView(binding.root)
            //insert in db
            sessionViewModel.insert(session)

            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    override fun onExerciseAdded(
        position: Int,
        isLast: Boolean,
        divider: View,
        llSessions: LinearLayoutCompat
    ) {
        divider.visibility = View.GONE

        if ((isAddExerciseClicked && isLast) || !isAddExerciseClicked) {
            isAddExerciseClicked = false

            val sessions: MutableList<Session> =
                sessionViewModel.getSessions(exercises[position].id)

            Log.d(TAG, "onExerciseAdded: ")

            if (sessions.size == 0) {
                divider.visibility = View.GONE
            } else {
                divider.visibility = View.VISIBLE
            }

            sessionMap[exercises[position].id] = llSessions
            for (session in sessions) {
                val binding: CardSessionBinding =
                    DataBindingUtil.inflate(layoutInflater, R.layout.card_session, null, false)
                binding.session = session

                val layoutParams: LinearLayoutCompat.LayoutParams =
                    LinearLayoutCompat.LayoutParams(
                        LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                        LinearLayoutCompat.LayoutParams.MATCH_PARENT
                    )
                layoutParams.setMargins(30, 0, 30, 20)
                binding.root.layoutParams = layoutParams
                binding.workClick = this
                binding.restClick = this
                llSessions.addView(binding.root)
            }
        }
    }

    override fun onWorkClicked(view: View, session: Session) {
        if (isWorkoutRunning) {
            isWork = true
//            isSessionClicked = true
            val cardSession: View = view.parent.parent as View
            val llSessions: LinearLayoutCompat = cardSession.parent as LinearLayoutCompat
            val sessionIndex = llSessions.indexOfChild(cardSession)
            seconds = session.workTime!!
            timer.cancel()
            startTimer()

            currSessionPosition = sessionIndex
            currExerciseId = session.exerciseId!!
            val exercise: Exercise = exerciseViewModel.getExerciseById(currExerciseId)
            currExerciseName = exercise.exerciseName!!

            for ((count, currExercise) in exercises.withIndex()) {
                if (currExercise.id == (currExerciseId)) {
                    currExercisePosition = count
                    break
                }
            }
            resetAnimation(true)
            animateView(0)
        }
    }

    override fun onRestClicked(view: View, session: Session) {
        if (isWorkoutRunning) {
//            isSessionClicked = true
            val cardSession: View = view.parent.parent as View
            val llSessions: LinearLayoutCompat = cardSession.parent as LinearLayoutCompat
            val sessionIndex = llSessions.indexOfChild(cardSession)
            seconds = session.restTime!!
            dataMap
            sessionMap
            viewMap

            currentSession = session
            currSessionPosition = sessionIndex
            isWork = false
            currExerciseId = session.exerciseId!!
            val exercise: Exercise = exerciseViewModel.getExerciseById(currExerciseId)
            currExerciseName = exercise.exerciseName!!

            for ((count, currExercise) in exercises.withIndex()) {
                if (currExercise.id == (currExerciseId)) {
                    currExercisePosition = count
                    break
                }
            }

            timer.cancel()
            startTimer()
            resetAnimation(false)
            animateView(0)
        }
    }

    private fun stopWorkoutDialog(exit: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Stop Workout?")
            .setPositiveButton(
                "Yes"
            ) { dialog, id ->
                Log.d(TAG, "stopWorkoutDialog: Yes")
                isWorkoutRunning = false
                llTimer.visibility = View.GONE
                stopTimer()
                if (exit == "exit") {
                    finish()
                } else {
                    clParentAddPlay.visibility = View.VISIBLE
                }
            }
            .setNegativeButton(
                "No"
            ) { dialog, id ->
                Log.d(TAG, "stopWorkoutDialog: Cancel")
                dialog.dismiss()
            }
        // Create the AlertDialog object and return it
        val dialog: AlertDialog = builder.create()
//        dialog.setTitle("title")
        dialog.show()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.cl_add_exercise -> {
                isAddExerciseClicked = true
                addExerciseDialog()
            }
            R.id.img_stop -> {
                stopWorkoutDialog("")
            }
            R.id.tv_play -> {
                Log.d(TAG, "onClick: Play")
                llTimer.visibility = View.VISIBLE
                setMap()
                startTimer()
                clParentAddPlay.visibility = View.GONE
            }
            R.id.img_lock -> {
                Log.d(TAG, "onClick: Lock")
            }
            R.id.img_pause -> {
                Log.d(TAG, "onClick: Pause")
                if (isTimerRunning) {
                    pauseTimer()
                } else {
                    startTimer()
                }
            }
            R.id.img_replay -> {
                Log.d(TAG, "onClick: Replay")
            }
            R.id.img_forward -> {
                Log.d(TAG, "onClick: Forward")
            }
        }
    }

    override fun onBackPressed() {
        if (isWorkoutRunning) stopWorkoutDialog("exit") else finish()
    }
}