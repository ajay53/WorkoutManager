package com.goazzi.workoutmanager.view.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.goazzi.workoutmanager.R
import com.goazzi.workoutmanager.adapter.WorkoutListAdapter
import com.goazzi.workoutmanager.helper.Util
import com.goazzi.workoutmanager.model.Exercise
import com.goazzi.workoutmanager.model.Session
import com.goazzi.workoutmanager.model.Workout
import com.goazzi.workoutmanager.view.activity.ExerciseActivity
import com.goazzi.workoutmanager.viewmodel.ExerciseViewModel
import com.goazzi.workoutmanager.viewmodel.SessionViewModel
import com.goazzi.workoutmanager.viewmodel.WorkoutViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WorkoutFragment : Fragment(), WorkoutListAdapter.OnWorkoutCLickListener,
    View.OnClickListener {

    companion object {
        private const val TAG = "WorkoutFragment"
    }

    private lateinit var fragmentActivity: FragmentActivity
    private lateinit var applicationContext: Context
    private lateinit var viewModel: WorkoutViewModel
    private lateinit var root: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")
        fragmentActivity = requireActivity()
        applicationContext = fragmentActivity.applicationContext
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        root = inflater.inflate(R.layout.fragment_workout, container, false)
        initViews()
        return root
    }

    private fun initViews() {
        viewModel = ViewModelProvider(this).get(WorkoutViewModel::class.java)
        val fabAddWorkout = root.findViewById<FloatingActionButton>(R.id.fab_add_workout)
        val rvWorkout = root.findViewById<RecyclerView>(R.id.rv_workout)
        fabAddWorkout?.setOnClickListener(this)

        //set recycler view
        viewModel.getLiveWorkout.observe(viewLifecycleOwner, { workouts ->
            if (viewModel.adapter == null) {
                viewModel.workouts = workouts
                viewModel.adapter = WorkoutListAdapter(applicationContext, viewModel.getLiveWorkout.value!!, this)
                rvWorkout?.adapter = viewModel.adapter
                rvWorkout?.layoutManager = LinearLayoutManager(applicationContext)
                rvWorkout?.setHasFixedSize(true)
            } else {
                if (viewModel.workouts.size < workouts.size) {
                    viewModel.adapter?.add(workouts[viewModel.swipedPosition], viewModel.swipedPosition)
                } else {
                    viewModel.adapter?.delete(viewModel.swipedPosition)
                }

                viewModel.workouts = workouts
            }
        })
        ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(rvWorkout)
    }

    override fun onWorkoutClick(position: Int) {
//        Util.showSnackBar(root, "Workout: " + workouts[position].name)
        val intent = Intent(applicationContext, ExerciseActivity::class.java).putExtra("id", viewModel.workouts[position].id)
                .putExtra("name", viewModel.workouts[position].name)
        fragmentActivity.startActivity(intent)
    }

    override fun onMenuClick(position: Int) {
        Log.d(TAG, "onMenuClick: ")
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fab_add_workout -> {
                if (!viewModel.isFabClicked) {
                    viewModel.isFabClicked = true
                    addWorkoutDialog()
                }
            }
        }
    }

    private fun addWorkoutDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(fragmentActivity)
        val viewGroup: ViewGroup = root.findViewById(R.id.fragment_workout)
        val view: View = LayoutInflater.from(applicationContext)
                .inflate(R.layout.dialog_add_workout, viewGroup, false)
        val edtWorkoutName = view.findViewById<EditText>(R.id.edt_workout_name)
        val btnSave = view.findViewById<Button>(R.id.btn_save)
        builder.setView(view)
        val alertDialog: AlertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnSave.setOnClickListener {
            if (edtWorkoutName.text.toString()
                        .isBlank()) {
                Util.showSnackBar(root.findViewById<RelativeLayout>(R.id.fragment_workout), getString(R.string.empty_name))
            } else {
                viewModel.swipedPosition = viewModel.workouts.size
                viewModel.insert(Workout(Util.getUUID(), edtWorkoutName.text.toString()
                        .uppercase(), Util.getTimeStamp()))
                alertDialog.dismiss()
            }
        }
        alertDialog.show()
        alertDialog.setOnDismissListener {
            viewModel.isFabClicked = false
        }
    }

    private var simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
            viewModel.swipedPosition = viewHolder.bindingAdapterPosition
            val executor: ExecutorService = Executors.newSingleThreadExecutor()
            executor.execute(kotlinx.coroutines.Runnable {
                deleteWorkout(viewHolder.bindingAdapterPosition)
            })
        }
    }

    private fun deleteWorkout(position: Int) {
        val workout: Workout = viewModel.workouts[position]
        val exerciseViewModel: ExerciseViewModel = ViewModelProvider(this).get(ExerciseViewModel::class.java)
        val sessionViewModel: SessionViewModel = ViewModelProvider(this).get(SessionViewModel::class.java)

        val exercises: List<Exercise> = exerciseViewModel.getExercisesById(workout.id)
        val sessions: MutableList<Session> = mutableListOf()

        exercises.forEach { exercise ->
            val currSessions: List<Session> = sessionViewModel.getSessionsById(exercise.id)
            sessions.addAll(currSessions)
            currSessions.forEach { session ->
                sessionViewModel.delete(session)
            }
            exerciseViewModel.delete(exercise)
        }
        viewModel.delete(workout)
        val handler = Handler(Looper.getMainLooper())
        handler.post(kotlinx.coroutines.Runnable {
            showSnackBar(workout, exercises, sessions, exerciseViewModel, sessionViewModel)
        })
    }

    private fun showSnackBar(workout: Workout, exercises: List<Exercise>, sessions: List<Session>, exerciseViewModel: ExerciseViewModel, sessionViewModel: SessionViewModel) {
        Snackbar.make(root.findViewById<RelativeLayout>(R.id.fragment_workout), getString(R.string.workout_deleted), Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.undo)) {
                    Log.d(TAG, "showSnackBar: UNDO clicked")

                    sessions.forEach { session ->
                        sessionViewModel.insert(session)
                    }
                    exercises.forEach { exercise ->
                        exerciseViewModel.insert(exercise)
                    }
                    viewModel.insert(workout)
                }
                .show()
    }


}