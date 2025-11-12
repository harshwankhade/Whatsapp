package com.github.OOADGroup9.EchoApp;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class ChatsFragment extends Fragment
{
    private View PrivateChatsView;
    private RecyclerView chatsList;

    private DatabaseReference ChatsRef, UsersRef;
    private FirebaseAuth mAuth;
    private String currentUserID="";
    private Map<String, ValueEventListener> userListenersMap = new HashMap<>();

    public ChatsFragment()
    {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        PrivateChatsView = inflater.inflate(R.layout.fragment_chats, container, false);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            return null;
        }
        currentUserID = mAuth.getCurrentUser().getUid();
        ChatsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        chatsList = (RecyclerView) PrivateChatsView.findViewById(R.id.chats_list);
        chatsList.setLayoutManager(new LinearLayoutManager(getContext()));

        return PrivateChatsView;
    }
    @Override
    public void onStart()
    {
        super.onStart();


        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(ChatsRef, Contacts.class)
                        .build();


        FirebaseRecyclerAdapter<Contacts, ChatsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, ChatsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final ChatsViewHolder holder, int position, @NonNull Contacts model)
                    {
                        final String usersIDs = getRef(position).getKey();
                        final String[] retImage = {"default_image"};
                        final String[] retName = {"User"};

                        // Remove old listener if it exists
                        if (userListenersMap.containsKey(usersIDs)) {
                            UsersRef.child(usersIDs).removeEventListener(userListenersMap.get(usersIDs));
                        }

                        // Create a new ValueEventListener that updates whenever the user data changes
                        ValueEventListener userListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                if (dataSnapshot.exists())
                                {
                                    if (dataSnapshot.hasChild("image"))
                                    {
                                        retImage[0] = dataSnapshot.child("image").getValue().toString();
                                        if (retImage[0] != null && !retImage[0].isEmpty()) {
                                            Picasso.get().load(retImage[0]).into(holder.profileImage);
                                        }
                                    }

                                    if (dataSnapshot.hasChild("name"))
                                    {
                                        retName[0] = dataSnapshot.child("name").getValue().toString();
                                        holder.userName.setText(retName[0]);
                                    }

                                    // Update user status - this is the key part that checks if online/offline
                                    if (dataSnapshot.hasChild("userState"))
                                    {
                                        if (dataSnapshot.child("userState").hasChild("state"))
                                        {
                                            String state = dataSnapshot.child("userState").child("state").getValue().toString();
                                            
                                            if (state != null) {
                                                if (state.equals("online"))
                                                {
                                                    holder.userStatus.setText("online");
                                                }
                                                else if (state.equals("offline"))
                                                {
                                                    String date = "";
                                                    String time = "";
                                                    
                                                    if (dataSnapshot.child("userState").hasChild("date")) {
                                                        date = dataSnapshot.child("userState").child("date").getValue().toString();
                                                    }
                                                    if (dataSnapshot.child("userState").hasChild("time")) {
                                                        time = dataSnapshot.child("userState").child("time").getValue().toString();
                                                    }
                                                    
                                                    holder.userStatus.setText("Last Seen: " + date + " " + time);
                                                }
                                            }
                                        }
                                    }
                                    else
                                    {
                                        holder.userStatus.setText("offline");
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        };

                        // Store the listener and add it to continuously monitor user state changes
                        userListenersMap.put(usersIDs, userListener);
                        UsersRef.child(usersIDs).addValueEventListener(userListener);

                        // Set click listener outside of ValueEventListener to ensure it's always set
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view)
                            {
                                Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                chatIntent.putExtra("visit_user_id", usersIDs);
                                chatIntent.putExtra("visit_user_name", retName[0]);
                                chatIntent.putExtra("visit_image", retImage[0]);
                                startActivity(chatIntent);
                            }
                        });
                    }

                    @NonNull
                    @Override
                    public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
                    {
                        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout, viewGroup, false);
                        return new ChatsViewHolder(view);
                    }
                };

        if (chatsList != null) {
            chatsList.setAdapter(adapter);
            adapter.startListening();
        }
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        // Remove all listeners to prevent memory leaks
        for (String userId : userListenersMap.keySet()) {
            UsersRef.child(userId).removeEventListener(userListenersMap.get(userId));
        }
        userListenersMap.clear();
    }


    public static class  ChatsViewHolder extends RecyclerView.ViewHolder
    {
        CircleImageView profileImage;
        TextView userStatus, userName;

        public ChatsViewHolder(@NonNull View itemView)
        {
            super(itemView);

            profileImage = itemView.findViewById(R.id.users_profile_image);
            userStatus = itemView.findViewById(R.id.user_status);
            userName = itemView.findViewById(R.id.user_profile_name);
        }
    }
}
