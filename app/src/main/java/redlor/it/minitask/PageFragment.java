package redlor.it.minitask;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import redlor.it.minitask.Utils.SimpleDividerItemDecoration;
import redlor.it.minitask.viewholder.FirebaseViewHolder;

import static redlor.it.minitask.MainActivity.mTwoPane;


public class PageFragment extends Fragment {

    DatabaseReference mDatabaseReference;
    FirebaseRecyclerAdapter mFirebaseAdapter;
    private RecyclerView mRecyclerView;
    private View view;
    private Toast mToast;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // inflate the tab view with these fragments
        view = inflater.inflate(R.layout.fragment_page, container, false);
        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String uid = firebaseUser.getUid();

        mDatabaseReference = FirebaseDatabase.getInstance().getReference("users").child(uid).child("toDoItems");
        mDatabaseReference.keepSynced(true);

        final Query query = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("toDoItems")
                .limitToLast(50);

        FirebaseRecyclerOptions<ToDoItem> options =
                new FirebaseRecyclerOptions.Builder<ToDoItem>()
                        .setQuery(query, ToDoItem.class)
                        .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<ToDoItem, FirebaseViewHolder>(options
        ) {

            @Override
            public FirebaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.todo_item, parent, false);
                return new FirebaseViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(final FirebaseViewHolder viewHolder, final int position, final ToDoItem toDoItem) {
                // set the content of the item
                viewHolder.content.setText(toDoItem.getContent());
                // set the checkbox status of the item
                viewHolder.checkDone.setChecked(toDoItem.getDone());
                // check if checkbox is checked, then strike through the text
                // this is for the first time UI render
                if (viewHolder.checkDone.isChecked()) {
                    viewHolder.content.setPaintFlags(viewHolder.content.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    viewHolder.content.setPaintFlags(viewHolder.content.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                }
                // render the clock icon if the item has a reminder
                // and add a animation for expired item
                if (toDoItem.getHasReminder()) {
                    viewHolder.clockReminder.setVisibility(View.VISIBLE);
                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String currentDateTime = sdf.format(c.getTime());
                    if (currentDateTime.compareTo(toDoItem.getReminderDate()) > 0) {
                        TranslateAnimation animation = new TranslateAnimation(0, 0, 0, 6);
                        animation.setDuration(400);
                        animation.setInterpolator(new LinearInterpolator());
                        animation.setRepeatCount(Animation.INFINITE);
                        viewHolder.clockReminder.setAnimation(animation);
                    }
                } else {
                    viewHolder.clockReminder.setVisibility(View.INVISIBLE);
                }

                viewHolder.checkDone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, final boolean b) {
                        final String id = mFirebaseAdapter.getRef(position).getKey();

                        if (b) {
                            toDoItem.setDone(true);
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("done", true);
                            mDatabaseReference.child(id).updateChildren(map);
                            viewHolder.checkDone.setOnCheckedChangeListener(null);

                        } else {
                            toDoItem.setDone(false);
                            HashMap<String, Object> map = new HashMap<>();
                            map.put("done", false);
                            mDatabaseReference.child(id).updateChildren(map);
                            viewHolder.checkDone.setOnCheckedChangeListener(null);

                        }
                    }
                });

                viewHolder.setOnClickListener(new FirebaseViewHolder.ClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        final String itemId = mFirebaseAdapter.getRef(position).getKey();
                        if (mTwoPane) {
                            DetailFragment newDetailFragment = new DetailFragment();
                            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                            Bundle bundle = new Bundle();
                            bundle.putString("content", toDoItem.getContent());
                            bundle.putString("reminder", toDoItem.getReminderDate());
                            bundle.putBoolean("hasReminder", toDoItem.getHasReminder());
                            bundle.putBoolean("done", toDoItem.getDone());
                            bundle.putString("itemId", itemId);
                            newDetailFragment.setArguments(bundle);

                            fragmentManager.beginTransaction()
                                    .replace(R.id.details_container, newDetailFragment)
                                    .detach(newDetailFragment)
                                    .attach(newDetailFragment)
                                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                    .commit();
                        } else {
                            Intent intent = new Intent(getContext(), DetailActivity.class);
                            intent.putExtra("content", toDoItem.getContent());
                            intent.putExtra("reminder", toDoItem.getReminderDate());
                            intent.putExtra("hasReminder", toDoItem.getHasReminder());
                            intent.putExtra("done", toDoItem.getDone());
                            intent.putExtra("itemId", itemId);
                            System.out.println("id in page fragment: " + itemId);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onItemLongClick(View view, int position) {
                        if (mToast != null) {
                            mToast.cancel();
                        }
                        boolean hasReminder = toDoItem.getHasReminder();
                        if (hasReminder) {
                            String reminder = toDoItem.getReminderDate();
                            mToast = Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.reminder_info) + " " + reminder, Toast.LENGTH_SHORT);
                            mToast.show();
                        } else {
                            mToast = Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.no_reminder), Toast.LENGTH_SHORT);
                            mToast.show();
                        }
                    }
                });
            }
        };

        mRecyclerView = view.findViewById(R.id.to_do_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getContext()));
        mRecyclerView.setAdapter(mFirebaseAdapter);

    }

    @Override
    public void onStop() {
        super.onStop();
        mFirebaseAdapter.stopListening();

    }

    @Override
    public void onStart() {
        super.onStart();
        mFirebaseAdapter.startListening();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
