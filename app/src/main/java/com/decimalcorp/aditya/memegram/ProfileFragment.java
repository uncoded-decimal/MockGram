package com.decimalcorp.aditya.memegram;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.decimalcorp.aditya.memegram.MainActivity.user;

public class ProfileFragment extends Fragment {
    TextView tv;
    Context context;
    FirebaseAuth mAuth;
    FirebaseUser mUser = user;
    DatabaseReference mDatabaseReference;
    GoogleSignInClient mGoogleSignInClient;
    Map<String,String> map;
    List<String> urls;
    RecyclerView recyclerView;
    StorageReference storageReference;
    int c=0;

    @Override
    public void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        mUser=mAuth.getCurrentUser();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile_fragment, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context=getContext();
        storageReference=FirebaseStorage.getInstance().getReferenceFromUrl("gs://memegram-4576b.appspot.com/UserUploads/UserUploads");
        recyclerView=view.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(context,3));
        recyclerView.setAdapter(new RecyclerViewAdapter(context));
        map=new HashMap<String,String>();
        if(mUser!=null){
            mDatabaseReference= FirebaseDatabase.getInstance().getReference();
            String s= user.getEmail();
            String x=s.substring(0,s.indexOf('@'));
            mDatabaseReference=mDatabaseReference.child(x);
        }
        DatabaseReference cRef = mDatabaseReference.child("Count");
        DatabaseReference upRef = mDatabaseReference.child("Uploads");
        urls = new ArrayList<>();
        //urls.add(0,"https://vignette.wikia.nocookie.net/starwars/images/3/36/Ezra_Rebels_Season_3.png/revision/latest/scale-to-width-down/500?cb=20161118033548");

        if(cRef!=null)
            cRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.getValue(Integer.class) != null) {
                        c = dataSnapshot.getValue(Integer.class);
                        Toast.makeText(getActivity(), "C = " + c, Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                   // Toast.makeText(getActivity(),"Updation Cancelled\nC = "+c,Toast.LENGTH_SHORT).show();
                }
            });
        if(upRef!=null)
            upRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                        map.put(postSnapshot.getKey(), (String) postSnapshot.getValue());
                    }
                    urls=new ArrayList<>(map.values());
                    Toast.makeText(getActivity(),"Hashmap length="+urls.size(),Toast.LENGTH_SHORT).show();
                    recyclerView.setAdapter(new RecyclerViewAdapter(context));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    //Toast.makeText(getActivity(),"Hashmap Updation Cancelled",Toast.LENGTH_SHORT).show();
                }
            });


        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(context);

        if (acct != null) {
            String personName = acct.getDisplayName();
            String personGivenName = acct.getGivenName();
            String personFamilyName = acct.getFamilyName();
            String personEmail = acct.getEmail();
            String personId = acct.getId();
            Uri personPhoto = acct.getPhotoUrl();

            ImageView imageView = view.findViewById(R.id.profile_pic);
            Picasso.with(getActivity()).load(personPhoto).into(imageView);
            TextView name = view.findViewById(R.id.name);
            name.setText(personName);

        }
       GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient=GoogleSignIn.getClient(getActivity(),gso);
        Button button = (Button)view.findViewById(R.id.sign_out);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               signOut();
            }
        });
    }

    public void signOut(){
        FirebaseAuth.getInstance().signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(context,"Log Out Successful", Toast.LENGTH_SHORT).show();

            }
        });
        if(mAuth==null){
            Toast.makeText(getActivity(),"Firebase logout Successful",Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(getActivity(),"Firebase disengage Successful",Toast.LENGTH_SHORT).show();
        }
        getActivity().finish();
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
        LayoutInflater inflater;

        public RecyclerViewAdapter(Context c){
            inflater=LayoutInflater.from(c);
        }

        @NonNull
        @Override
        public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = inflater.inflate(R.layout.recyclerview_item,parent,false);
            itemView.setMinimumHeight(150);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position) {
            if(urls!=null){
            //Uri uri = Uri.parse(urls.get(position));
            //Toast.makeText(getContext(),""+urls.get(position),Toast.LENGTH_SHORT).show();
            Picasso.with(context)
                    .load(urls.get(position))
                    .transform(new CropSquareTransformation())
                    .into(holder.view);
            }
        }

        @Override
        public int getItemCount() {
            if(urls != null)
                return urls.size();
            else
                return 0;
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            ImageView view;

            public ViewHolder(View itemView) {
                super(itemView);
                view=itemView.findViewById(R.id.image_recycler);
            }

            @Override
            public void onClick(View v) {

            }
        }
    }

    public class CropSquareTransformation implements Transformation {
        @Override public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;
            Bitmap result = Bitmap.createBitmap(source, x, y, size, size);
            if (result != source) {
                source.recycle();
            }
            return result;
        }

        @Override public String key() { return "square()"; }
    }
}
