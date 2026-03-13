package esports.model;

public class MatchRecord {

    private int id;
    private String matchDate;
    private String matchDuration;
    private String tournament;
    private String game;
    private String format;
    private String team1;
    private String team2;
    private String winner;

    public MatchRecord() {}

    public MatchRecord(int id, String matchDate, String matchDuration, String tournament,
                       String game, String format, String team1, String team2, String winner) {
        this.id = id;
        this.matchDate = matchDate;
        this.matchDuration = matchDuration;
        this.tournament = tournament;
        this.game = game;
        this.format = format;
        this.team1 = team1;
        this.team2 = team2;
        this.winner = winner;
    }

    public int getId()              { return id; }
    public String getMatchDate()    { return matchDate; }
    public String getMatchDuration(){ return matchDuration; }
    public String getTournament()   { return tournament; }
    public String getGame()         { return game; }
    public String getFormat()       { return format; }
    public String getTeam1()        { return team1; }
    public String getTeam2()        { return team2; }
    public String getWinner()       { return winner; }

    public void setId(int id)                        { this.id = id; }
    public void setMatchDate(String matchDate)        { this.matchDate = matchDate; }
    public void setMatchDuration(String matchDuration){ this.matchDuration = matchDuration; }
    public void setTournament(String tournament)      { this.tournament = tournament; }
    public void setGame(String game)                  { this.game = game; }
    public void setFormat(String format)              { this.format = format; }
    public void setTeam1(String team1)               { this.team1 = team1; }
    public void setTeam2(String team2)               { this.team2 = team2; }
    public void setWinner(String winner)             { this.winner = winner; }
}
