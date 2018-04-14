package com.example.zach.smashmyandroid.activities.Player;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zach.smashmyandroid.R;
import com.example.zach.smashmyandroid.database.local.DataSource.PlayerDataSource;
import com.example.zach.smashmyandroid.database.local.Repository.MatchRepository;
import com.example.zach.smashmyandroid.database.SmaDatabase;
import com.example.zach.smashmyandroid.database.local.DataSource.MatchDataSource;
import com.example.zach.smashmyandroid.database.local.Repository.PlayerRepository;
import com.example.zach.smashmyandroid.database.local.models.Match;
import com.example.zach.smashmyandroid.database.local.models.Player;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class PlayerProfile extends AppCompatActivity {

    SmaDatabase mDb;

    TextView name;
    TextView smashName;
    TextView rank;

    private ListView lvMatches;

    List<Match> matchList = new ArrayList<>();
    ArrayAdapter adapter;

    private Player p;

    private CompositeDisposable mCompositeDisposable;
    private MatchRepository matchRepository;
    private PlayerRepository playerRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_profile);

        name = findViewById(R.id.name);
        smashName = findViewById(R.id.smashName);
        rank = findViewById(R.id.rank);

        final Player player = getIntent().getExtras().getParcelable("player");

        name.setText(player.getFirstName().toString() + " " + player.getLastName().toString());
        smashName.setText(player.getSmashName().toString());
        rank.setText(Integer.toString(player.getRank()));

        mCompositeDisposable = new CompositeDisposable();

        lvMatches = (ListView) findViewById(R.id.matches);


        mDb = SmaDatabase.getInstance(this);
        matchRepository = MatchRepository.getInstance(MatchDataSource.getInstance(mDb.matchDao()));
        playerRepository = PlayerRepository.getInstance(PlayerDataSource.getInstance(mDb.playerDao()));
        loadData(player.getId());

        // Create new array adapter
        adapter = new ArrayAdapter<Match>(this, 0, matchList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Match m = matchList.get(position);
                final Player[] loser = new Player[1];
                final Player[] winner = new Player[1];
                Observable.just(mDb).subscribeOn(Schedulers.io()).subscribe(smaDb -> winner[0] = playerRepository.loadUserById(m.getWinnerId()));
                Observable.just(mDb).subscribeOn(Schedulers.io()).subscribe(smaDb -> loser[0] = playerRepository.loadUserById(m.getLoserId()));


                if (convertView == null) {
                    convertView = getLayoutInflater()
                            .inflate(R.layout.match_list_item, null, false);
                }
                TextView winnerName = convertView.findViewById(R.id.winnerName);
                TextView loserName = convertView.findViewById(R.id.loserName);

                winnerName.setText(winner[0].getSmashName());
                loserName.setText(loser[0].getSmashName());

                return convertView;
            }
        };

        // Register list view for use with context menus
        registerForContextMenu(lvMatches);

        // Assign adapter to the listView
        lvMatches.setAdapter(adapter);

    }


    private void loadData(int id) {
        Disposable disposable = matchRepository.getMatchesByUser(id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<List<Match>>() {
                    @Override
                    public void accept(List<Match> matches) throws Exception {
                        onGetPlayerMatchesSuccess(matches);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Toast.makeText(PlayerProfile.this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        mCompositeDisposable.add(disposable);
    }

    private void onGetPlayerMatchesSuccess(List<Match> matches) {
        matchList.clear();;
        matchList.addAll(matches);
        adapter.notifyDataSetChanged();
    }
}

