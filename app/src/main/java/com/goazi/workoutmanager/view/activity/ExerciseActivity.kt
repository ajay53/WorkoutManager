package com.goazi.workoutmanager.view.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.Button
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.goazi.workoutmanager.R
import com.goazi.workoutmanager.adapter.ExerciseListAdapter
import com.goazi.workoutmanager.background.SilentForegroundService
import com.goazi.workoutmanager.databinding.CardSessionBinding
import com.goazi.workoutmanager.helper.Util
import com.goazi.workoutmanager.model.Exercise
import com.goazi.workoutmanager.model.Session
import com.goazi.workoutmanager.viewmodel.ExerciseViewModel
import com.goazi.workoutmanager.viewmodel.SessionViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ExerciseActivity : AppCompatActivity(), ExerciseListAdapter.OnExerciseCLickListener,
    View.OnClickListener, PopupMenu.OnMenuItemClickListener, Util.WorkOnClick, Util.RestOnClick,
    Util.DeleteOnClick {
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
    private lateinit var imgStop: AppCompatImageView
    private lateinit var imgLock: AppCompatImageView
    private lateinit var imgRewind: AppCompatImageView
    private lateinit var imgPause: AppCompatImageView
    private lateinit var imgForward: AppCompatImageView

    //variables
//    private lateinit var smoothScroller: SmoothScroller
//    private lateinit var exercises: List<Exercise>
//    private var exerciseCount: Int = 0
    private lateinit var viewModel: ExerciseViewModel
    private lateinit var sessionViewModel: SessionViewModel

    //    private lateinit var workoutId: String
//    private var isAddExerciseClicked: Boolean = false
//    private var isTimerRunning: Boolean = false
//    private var seconds: Long = 10
//    private var currExerciseName: String = ""
//    private var currExerciseId: String = ""
//    private var currExercisePosition: Int = 0
//    private var currSessionPosition: Int = -1
//    private lateinit var currentSession: Session
//    private var isWork: Boolean = false
//    private var isWorkoutRunning: Boolean = false
//    private var isLocked: Boolean = false
//    private lateinit var timer: CountDownTimer
//    private var dataMap: MutableMap<String?, MutableList<Session>> = HashMap()
//    private var viewMap: MutableMap<String?, MutableList<View>> = LinkedHashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_exercise)

        viewModel = ViewModelProvider(this).get(ExerciseViewModel::class.java)
        sessionViewModel = ViewModelProvider(this).get(SessionViewModel::class.java)
        viewModel.workoutId = intent.extras!!.getString("id")
                .toString()
        viewModel.searchById(viewModel.workoutId)

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

        imgRewind = findViewById(R.id.img_rewind)
        imgRewind.setOnClickListener(this)

        imgPause = findViewById(R.id.img_pause)
        imgPause.setOnClickListener(this)

        imgForward = findViewById(R.id.img_forward)
        imgForward.setOnClickListener(this)

        tvSeconds = findViewById(R.id.tv_seconds)
        viewModel.seconds = tvSeconds.text.toString()
                .toLong() * 1000

        llTimer = findViewById(R.id.ll_timer)
        tvExerciseName = findViewById(R.id.tv_exercise_name)
        val tvWorkoutName = findViewById<TextView>(R.id.tv_workout_name)
        tvWorkoutName.text = intent.extras!!.getString("name")
                .toString()

        rvExercise = findViewById(R.id.rv_exercise)
        var adapter = ExerciseListAdapter(applicationContext, viewModel.getLiveExercisesById.value, this)

        viewModel.getLiveExercisesById.observe(this, Observer { exercises ->
            if (exercises.size > 0) {
                viewModel.currExerciseName = exercises[0].exerciseName
                viewModel.currExerciseId = exercises[0].id
            }
            viewModel.exercises = exercises
            if (viewModel.exerciseCount == 0) {
                viewModel.exerciseCount = exercises.size
                adapter = ExerciseListAdapter(applicationContext, viewModel.getLiveExercisesById.value, this)
                rvExercise.adapter = adapter
                val layoutManager = LinearLayoutManager(applicationContext)
//                layoutManager.stackFromEnd = true
                rvExercise.layoutManager = layoutManager
                rvExercise.setHasFixedSize(false)

                rvExercise.viewTreeObserver.addOnGlobalLayoutListener(object :
                    OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (!viewModel.isAddExerciseClicked) {
//                                scrollToBottom()
                            scrollToTop()
                        }
                        rvExercise.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            } else {
                adapter.updateList(exercises)
            }
        })

        /*smoothScroller = object : LinearSmoothScroller(applicationContext) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }*/
    }

    private fun startTimer() {
        viewModel.timer = object : CountDownTimer(viewModel.seconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
//                Log.d(TAG, "onTick: ${viewModel.seconds / 1000}")
                viewModel.seconds = millisUntilFinished
                updateCountDownText()
            }

            override fun onFinish() {
                viewModel.isTimerRunning = false
                viewModel.isWorkoutRunning = true
                when {
                    viewModel.currSessionPosition < viewModel.dataMap[viewModel.currExerciseId]!!.size - 1 || (viewModel.currSessionPosition == viewModel.dataMap[viewModel.currExerciseId]!!.size - 1 && viewModel.isWork) -> {
                        viewModel.isWork = !viewModel.isWork
                        when {
                            viewModel.isWork -> viewModel.currSessionPosition++
                        }
                        tvExerciseName.text = viewModel.currExerciseName
                        viewModel.currentSession = viewModel.dataMap[viewModel.currExerciseId]!![viewModel.currSessionPosition]
                        viewModel.seconds = if (viewModel.isWork) viewModel.currentSession.workTime else viewModel.currentSession.restTime
                        viewModel.isTimerRunning = true
                        viewModel.timer.cancel()
                        Log.d(TAG, "Timer: cancel")
                        startTimer()
                        animateView()
                    }
                    viewModel.currExercisePosition < viewModel.exercises.size - 1 -> {
                        viewModel.currExercisePosition++
                        viewModel.currExerciseId = viewModel.exercises[viewModel.currExercisePosition].id
                        viewModel.currExerciseName = viewModel.exercises[viewModel.currExercisePosition].exerciseName
                        viewModel.currSessionPosition = 0
                        viewModel.currentSession = viewModel.dataMap[viewModel.currExerciseId]!![viewModel.currSessionPosition]
                        tvExerciseName.text = viewModel.currExerciseName
                        viewModel.isWork = !viewModel.isWork
                        viewModel.seconds = if (viewModel.isWork) viewModel.currentSession.workTime else viewModel.currentSession.restTime
                        viewModel.isTimerRunning = true
                        viewModel.timer.cancel()
                        Log.d(TAG, "Timer: cancel")
                        startTimer()
                        animateView()
                        rvExercise.scrollToPosition(viewModel.currExercisePosition)
                        /*rvExercise.smoothScrollToPosition(currExercisePosition)

                        rvExercise.layoutManager as LinearLayoutManager
                        val layoutManager: LinearLayoutManager =
                            rvExercise.layoutManager as LinearLayoutManager
                        layoutManager.scrollToPositionWithOffset(0, 0)*/

                        /*(rvExercise.layoutManager as LinearLayoutManager?)?.scrollToPositionWithOffset(
                            currExercisePosition,
                            0
                        )*/
                        /*smoothScroller.targetPosition = currExercisePosition
                        rvExercise.layoutManager?.startSmoothScroll(smoothScroller)*/
                    }
                    else -> {
                        stopTimer()
                    }
                }
            }
        }.start()
        Log.d(TAG, "Timer start")
        viewModel.isTimerRunning = true
        imgPause.setImageResource(R.drawable.ic_pause)
    }

    private fun pauseTimer() {
        imgPause.setImageResource(R.drawable.ic_play)
        viewModel.timer.cancel()
        Log.d(TAG, "Timer: cancel")
        viewModel.isTimerRunning = false
    }

    private fun stopTimer() {
        viewModel.timer.cancel()
        Log.d(TAG, "Timer: cancel")
        viewModel.isWorkoutRunning = false
        viewModel.isTimerRunning = false
        llTimer.visibility = View.GONE
        clParentAddPlay.visibility = View.VISIBLE

        viewModel.isWork = false
        viewModel.seconds = 5000
        viewModel.currExerciseName = ""
        viewModel.currExerciseId = ""
        viewModel.currExercisePosition = 0
        viewModel.currExerciseId = viewModel.exercises[0].id
        viewModel.currSessionPosition = 0
        Util.showSnackBar(findViewById(R.id.activity_exercise), "Workout Stopped")
        resetAnimation(true)

        viewModel.currSessionPosition = -1
        val intent = Intent().setClass(applicationContext, SilentForegroundService::class.java)
        stopService(intent)
    }

    private fun animateView() {
        val sessionList: MutableList<View> = viewModel.viewMap[viewModel.currExerciseId]!!
        val tv: TextView = if (viewModel.isWork) {
            sessionList[viewModel.currSessionPosition].findViewById(R.id.tv_work_time)
        } else {
            sessionList[viewModel.currSessionPosition].findViewById(R.id.tv_rest_time)
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
        val allSessionList: MutableList<MutableList<View>> = ArrayList(viewModel.viewMap.values)
        var sessionList: MutableList<View> = mutableListOf()
        for (i in 0 until viewModel.currExercisePosition + 1) {
            sessionList = allSessionList[i] // sessions in a exercise

            for (j in 0 until sessionList.size) {
                if (viewModel.currExercisePosition == i && viewModel.currSessionPosition == j) {
                    break
                }
                val workTv: TextView = sessionList[j].findViewById(R.id.tv_work_time)
                workTv.setTextColor(getColor(R.color.purple_700))
                val restTv: TextView = sessionList[j].findViewById(R.id.tv_rest_time)
                restTv.setTextColor(getColor(R.color.purple_700))
            }
        }

        if (!isWork) {
            val workTv: TextView = sessionList[viewModel.currSessionPosition].findViewById(R.id.tv_work_time)
            workTv.setTextColor(getColor(R.color.purple_700))
        }

        var tempSessionPosition: Int = viewModel.currSessionPosition
        if (!isWork) {
            tempSessionPosition++
        }

        for (i in viewModel.currExercisePosition until allSessionList.size) {
            sessionList = allSessionList[i] // sessions in a exercise

            for (j in tempSessionPosition until sessionList.size) {
                val workTv: TextView = sessionList[tempSessionPosition].findViewById(R.id.tv_work_time)
                workTv.setTextColor(getColor(R.color.white))
                val restTv: TextView = sessionList[tempSessionPosition].findViewById(R.id.tv_rest_time)
                restTv.setTextColor(getColor(R.color.white))
                tempSessionPosition++
            }
            tempSessionPosition = 0
        }
    }

    private fun setMap(exeId: String, llSession: LinearLayoutCompat) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        executor.execute(kotlinx.coroutines.Runnable {

            val sessions: MutableList<Session> = sessionViewModel.getSessionsById(exeId)
            viewModel.dataMap[exeId] = sessions

            val childCount: Int = llSession.childCount
            val sessionList: MutableList<View> = mutableListOf()
            for (i in 0 until childCount) {
                val ll: View = llSession.getChildAt(i)
                sessionList.add(ll)
            }
            viewModel.viewMap[exeId] = sessionList
            /*try {
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
            }*/
        })
    }

    private fun updateCountDownText() {
        val minutes = (viewModel.seconds / 1000).toInt() / 60
        val seconds = (viewModel.seconds / 1000).toInt() % 60
        val timeLeftFormatted: String = java.lang.String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
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
            val uuid = Util.getUUID()
            sessionViewModel.insert(Session(Util.getUUID(), 10000, 5000, Util.getTimeStamp(), uuid))
            viewModel.insert(Exercise(uuid, Util.getTimeStamp(), edtExerciseName.text.toString(), viewModel.workoutId))
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    override fun onExerciseClick(position: Int) {
        Log.d(TAG, "onExerciseClick: ")
    }

    override fun onMenuClick(position: Int, imgMenu: AppCompatImageView, llSessions: LinearLayoutCompat) {
//        addSessionDialog(position, llSessions)
        clickedLLSessions = llSessions
        clickedPosition = position

        val menu = PopupMenu(applicationContext, imgMenu)
        menu.menuInflater.inflate(R.menu.menu_edit_exercise, menu.menu)
        menu.gravity = Gravity.END
        menu.setOnMenuItemClickListener(this)
        menu.show()
    }

    private lateinit var clickedLLSessions: LinearLayoutCompat
    private var clickedPosition: Int = 0

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
            val session = Session(Util.getUUID(), edtWorkTime.text.toString()
                    .toLong() * 1000, edtRestTime.text.toString()
                    .toLong() * 1000, Util.getTimeStamp(), viewModel.exercises[position].id)
            //update UI
            val binding: CardSessionBinding = DataBindingUtil.inflate(layoutInflater, R.layout.card_session, null, false)
            binding.session = session

            val layoutParams: LinearLayoutCompat.LayoutParams = LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.MATCH_PARENT)
            layoutParams.setMargins(30, 0, 30, 20)
            binding.root.layoutParams = layoutParams
            binding.workClick = this
            binding.restClick = this
            binding.deleteClick = this
            llSessions.addView(binding.root)
            //insert in db
            sessionViewModel.insert(session)

            alertDialog.dismiss()
            setMap(viewModel.exercises[position].id, llSessions)
        }
        alertDialog.show()
    }

    override fun onExerciseAdded(position: Int, isLast: Boolean, llSessions: LinearLayoutCompat) {
        viewModel.isAddExerciseClicked = false

        val sessions: MutableList<Session> = sessionViewModel.getSessionsById(viewModel.exercises[position].id)

        llSessions.removeAllViews()
        for ((pos, session) in sessions.withIndex()) {
            val binding: CardSessionBinding = DataBindingUtil.inflate(layoutInflater, R.layout.card_session, null, false)
            binding.session = session

            val layoutParams: LinearLayoutCompat.LayoutParams = LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.MATCH_PARENT)
            layoutParams.setMargins(30, 0, 30, 20)
            binding.root.layoutParams = layoutParams
            val currSession = binding.root
            binding.workClick = this
            binding.restClick = this
            when {
                position < viewModel.currExercisePosition -> {
                    currSession.findViewById<TextView>(R.id.tv_work_time)
                            .setTextColor(getColor(R.color.purple_700))
                    currSession.findViewById<TextView>(R.id.tv_rest_time)
                            .setTextColor(getColor(R.color.purple_700))
                }
                position == viewModel.currExercisePosition -> {
                    when {
                        pos < viewModel.currSessionPosition -> {
                            currSession.findViewById<TextView>(R.id.tv_work_time)
                                    .setTextColor(getColor(R.color.purple_700))
                            currSession.findViewById<TextView>(R.id.tv_rest_time)
                                    .setTextColor(getColor(R.color.purple_700))
                        }
                        pos == viewModel.currSessionPosition -> {
                            when {
                                viewModel.isWork -> {
                                    currSession.findViewById<TextView>(R.id.tv_work_time)
                                            .setTextColor(getColor(R.color.purple_700))
                                }
                                else -> {
                                    currSession.findViewById<TextView>(R.id.tv_work_time)
                                            .setTextColor(getColor(R.color.purple_700))
                                    currSession.findViewById<TextView>(R.id.tv_rest_time)
                                            .setTextColor(getColor(R.color.purple_700))
                                }
                            }
                        }
                    }
                }
            }
            llSessions.addView(binding.root)
        }
        setMap(viewModel.exercises[position].id, llSessions)
        Log.d(TAG, "onExerciseAdded: position: $position")
    }

    override fun onWorkClicked(view: View, session: Session) {
        if (viewModel.isWorkoutRunning && !viewModel.isLocked) {
            Log.d(TAG, "onWorkClicked: ")
            viewModel.isWork = true
            val cardSession: View = view.parent.parent as View
            val llSessions: LinearLayoutCompat = cardSession.parent as LinearLayoutCompat
            val sessionIndex = llSessions.indexOfChild(cardSession)
            viewModel.seconds = session.workTime
            viewModel.timer.cancel()
            Log.d(TAG, "Timer: cancel")
            startTimer()

            viewModel.currentSession = session
            viewModel.currSessionPosition = sessionIndex
            viewModel.currExerciseId = session.exerciseId
            val exercise: Exercise = viewModel.getExerciseById(viewModel.currExerciseId)
            viewModel.currExerciseName = exercise.exerciseName

            for ((count, currExercise) in viewModel.exercises.withIndex()) {
                if (currExercise.id == (viewModel.currExerciseId)) {
                    viewModel.currExercisePosition = count
                    break
                }
            }

            resetAnimation(true)
            animateView()
        }
    }

    override fun onRestClicked(view: View, session: Session) {
        if (viewModel.isWorkoutRunning && !viewModel.isLocked) {
            Log.d(TAG, "onRestClicked: ")
            val cardSession: View = view.parent.parent as View
            val llSessions: LinearLayoutCompat = cardSession.parent as LinearLayoutCompat
            val sessionIndex = llSessions.indexOfChild(cardSession)
            viewModel.seconds = session.restTime

            viewModel.currentSession = session
            viewModel.currSessionPosition = sessionIndex
            viewModel.isWork = false
            viewModel.currExerciseId = session.exerciseId
            val exercise: Exercise = viewModel.getExerciseById(viewModel.currExerciseId)
            viewModel.currExerciseName = exercise.exerciseName

            for ((count, currExercise) in viewModel.exercises.withIndex()) {
                if (currExercise.id == (viewModel.currExerciseId)) {
                    viewModel.currExercisePosition = count
                    break
                }
            }

            viewModel.timer.cancel()
            Log.d(TAG, "Timer: cancel")
            startTimer()
            resetAnimation(false)
            animateView()
        }
    }

    override fun onDeleteClicked(view: View, session: Session) {
        Log.d(TAG, "onDeleteClicked: ")
    }

    private fun stopWorkoutDialog(exit: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Stop Workout?")
                .setPositiveButton("Yes") { dialog, id ->
                    Log.d(TAG, "stopWorkoutDialog: Yes")
                    viewModel.isWorkoutRunning = false
                    llTimer.visibility = View.GONE
                    stopTimer()
                    if (exit == "exit") {
                        finish()
                    } else {
                        clParentAddPlay.visibility = View.VISIBLE
                    }
                }
                .setNegativeButton("No") { dialog, id ->
                    Log.d(TAG, "stopWorkoutDialog: Cancel")
                    dialog.dismiss()
                }
        // Create the AlertDialog object and return it
        val dialog: AlertDialog = builder.create()
//        dialog.setTitle("title")
        dialog.show()
    }

    private fun showHideView() {
//        llTimer.animate().translationY(llTimer.measuredHeight.toFloat())
//        llTimer.animate().translationY(200F)
        llTimer.visibility = View.VISIBLE
//        expand(llTimer)
        clParentAddPlay.visibility = View.GONE
    }

    private fun expand(v: View) {
        val matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec((v.parent as View).width, View.MeasureSpec.EXACTLY)
        val wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
        val targetHeight = v.measuredHeight

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
//        v.layoutParams.height = 1
        v.visibility = View.VISIBLE
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                v.layoutParams.height = if (interpolatedTime == 1f) LinearLayoutCompat.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // Expansion speed of 1dp/ms
        a.duration = (targetHeight / v.context.resources.displayMetrics.density).toLong()
        v.startAnimation(a)
    }

    private fun deleteSession() {
        val session = viewModel.dataMap[viewModel.exercises[clickedPosition].id]?.get(viewModel.dataMap[viewModel.exercises[clickedPosition].id]?.size!! - 1)
        val view = clickedLLSessions[clickedLLSessions.childCount - 1]
        sessionViewModel.delete(session!!)
        runOnUiThread(kotlinx.coroutines.Runnable {
            clickedLLSessions.removeView(view)
            showSnackBar(getString(R.string.session_deleted), true, session, view, clickedLLSessions, null, null)
        })
    }

    private fun deleteExercise() {
        val exercise = viewModel.exercises[clickedPosition]
        val sessions: List<Session> = sessionViewModel.getSessionsById(exercise.id)
        viewModel.delete(exercise)
        for (set in sessions) {
            sessionViewModel.delete(set)
        }
        runOnUiThread(kotlinx.coroutines.Runnable {
            showSnackBar(getString(R.string.exercise_deleted), false, null, null, clickedLLSessions, exercise, sessions)
        })
    }

    private fun showSnackBar(msg: String, isSession: Boolean, session: Session?, view: View?, llSessions: LinearLayoutCompat, exercise: Exercise?, sessions: List<Session>?) {
        Snackbar.make(findViewById(R.id.activity_exercise), msg, Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.undo)) {
                    Log.d(TAG, "showSnackBar: UNDO clicked")
                    if (isSession) {
                        //undo session delete
                        sessionViewModel.insert(session!!)
                        llSessions.addView(view)
                    } else {
                        //undo exercise delete
                        if (sessions != null) {
                            for (set in sessions) {
                                sessionViewModel.insert(set)
                            }
                        }
                        viewModel.insert(exercise!!)
                    }
                }
                .show()
    }

    private fun rewind() {
        val time = if (viewModel.isWork) viewModel.currentSession.workTime else viewModel.currentSession.restTime
        if (viewModel.seconds + 10000 <= time) {
            viewModel.seconds += 10000
        } else {
            viewModel.seconds = time
        }
        viewModel.timer.cancel()
        Log.d(TAG, "Timer: cancel")
        startTimer()
    }

    private fun forward() {
        if (viewModel.seconds - 9000 >= 0) {
            viewModel.seconds -= 9000
            viewModel.timer.cancel()
            Log.d(TAG, "Timer: cancel")
            startTimer()
        } /*else {
            seconds = 0
        }*/
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.add_session -> {
                Log.d(TAG, "onMenuItemClick: add session")
                addSessionDialog(clickedPosition, clickedLLSessions)
                true
            }
            R.id.delete_set -> {
                Log.d(TAG, "onMenuItemClick: delete set")
                val executor: ExecutorService = Executors.newSingleThreadExecutor()
                executor.execute(kotlinx.coroutines.Runnable { deleteSession() })
                true
            }
            R.id.delete_exercise -> {
                Log.d(TAG, "onMenuItemClick: delete exercise")
                val executor: ExecutorService = Executors.newSingleThreadExecutor()
                executor.execute(kotlinx.coroutines.Runnable { deleteExercise() })
                true
            }
            else -> true
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.cl_add_exercise -> {
                viewModel.isAddExerciseClicked = true
                addExerciseDialog()
            }
            R.id.img_stop -> {
                stopWorkoutDialog("")
            }
            R.id.tv_play -> {
                Log.d(TAG, "onClick: Play")
                val intent = Intent().setClass(applicationContext, SilentForegroundService::class.java)
                startService(intent)
                showHideView()
                startTimer()
                scrollToTop()
            }
            R.id.img_lock -> {
                Log.d(TAG, "onClick: Lock")
                viewModel.isLocked = !viewModel.isLocked
                if (viewModel.isLocked) {
                    imgLock.setImageResource(R.drawable.ic_lock)
                } else {
                    imgLock.setImageResource(R.drawable.ic_unlock)
                }
            }
            R.id.img_pause -> {
                Log.d(TAG, "onClick: Pause")
                if (viewModel.isTimerRunning) {
                    pauseTimer()
                } else {
                    startTimer()
                }
            }
            R.id.img_rewind -> {
                Log.d(TAG, "onClick: Rewind")
                rewind()
            }
            R.id.img_forward -> {
                Log.d(TAG, "onClick: Forward")
                forward()
            }
        }
    }

    private fun scrollToTop() {
        viewModel.isAddExerciseClicked = false
        val layoutManager: LinearLayoutManager = rvExercise.layoutManager as LinearLayoutManager
        layoutManager.scrollToPositionWithOffset(0, 0)
    }

    private fun scrollToBottom() {
        rvExercise.scrollToPosition(viewModel.exercises.size - 1)
    }

    override fun onBackPressed() {
        if (viewModel.isWorkoutRunning) stopWorkoutDialog("exit") else finish()
    }
}