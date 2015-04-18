package au.com.mineauz.MobHunting.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import au.com.mineauz.MobHunting.MobHunting;
import au.com.mineauz.MobHunting.StatType;
import au.com.mineauz.MobHunting.util.UUIDHelper;

public class SQLiteDataStore extends DatabaseDataStore
{
	@Override
	protected Connection setupConnection() throws SQLException, DataStoreException
	{
		try
		{
			Class.forName("org.sqlite.JDBC"); //$NON-NLS-1$
			return DriverManager.getConnection("jdbc:sqlite:" + MobHunting.instance.getDataFolder().getPath() + "/" + MobHunting.config().databaseName + ".db"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		catch(ClassNotFoundException e)
		{
			throw new DataStoreException("SQLite not present on the classpath"); //$NON-NLS-1$
		}
	}

	@Override
	protected void setupTables(Connection connection) throws SQLException
	{
		Statement create = connection.createStatement();
		//Prefix tables to mh_
		try {
			ResultSet rs = create.executeQuery("SELECT * from Players LIMIT 0");
			rs.close();
			create.executeUpdate("ALTER TABLE Players RENAME TO mh_Players");
			create.executeUpdate("ALTER TABLE Achievements RENAME TO mh_Achievements");
			create.executeUpdate("ALTER TABLE Daily RENAME TO mh_Daily");
			create.executeUpdate("ALTER TABLE Weekly RENAME TO mh_Weekly");
			create.executeUpdate("ALTER TABLE Monthly RENAME TO mh_Monthly");
			create.executeUpdate("ALTER TABLE Yearly RENAME TO mh_Yearly");
			create.executeUpdate("ALTER TABLE AllTime RENAME TO mh_AllTime");
			
			create.executeUpdate("DROP TRIGGER IF EXISTS DailyInsert");
			create.executeUpdate("DROP TRIGGER IF EXISTS DailyUpdate");
			
		} catch (SQLException e) {
		}
		
		performTableMigrate(connection);
		
		create.executeUpdate("CREATE TABLE IF NOT EXISTS mh_PlayersNew (UUID TEXT PRIMARY KEY, NAME TEXT, PLAYER_ID INTEGER NOT NULL)"); //$NON-NLS-1$
		
		String dataString = ""; //$NON-NLS-1$
		for(StatType type : StatType.values())
			dataString += ", " + type.getDBColumn() + " INTEGER NOT NULL DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
		
		create.executeUpdate("CREATE TABLE IF NOT EXISTS mh_Daily (ID CHAR(6) NOT NULL, PLAYER_ID INTEGER REFERENCES mh_PlayersNew(PLAYER_ID)" + dataString + ", PRIMARY KEY(ID, PLAYER_ID))"); //$NON-NLS-1$ //$NON-NLS-2$
		create.executeUpdate("CREATE TABLE IF NOT EXISTS mh_Weekly (ID CHAR(6) NOT NULL, PLAYER_ID INTEGER REFERENCES mh_PlayersNew(PLAYER_ID)" + dataString + ", PRIMARY KEY(ID, PLAYER_ID))"); //$NON-NLS-1$ //$NON-NLS-2$
		create.executeUpdate("CREATE TABLE IF NOT EXISTS mh_Monthly (ID CHAR(6) NOT NULL, PLAYER_ID INTEGER REFERENCES mh_PlayersNew(PLAYER_ID)" + dataString + ", PRIMARY KEY(ID, PLAYER_ID))"); //$NON-NLS-1$ //$NON-NLS-2$
		create.executeUpdate("CREATE TABLE IF NOT EXISTS mh_Yearly (ID CHAR(6) NOT NULL, PLAYER_ID INTEGER REFERENCES mh_PlayersNew(PLAYER_ID)" + dataString + ", PRIMARY KEY(ID, PLAYER_ID))"); //$NON-NLS-1$ //$NON-NLS-2$
		create.executeUpdate("CREATE TABLE IF NOT EXISTS mh_AllTime (PLAYER_ID INTEGER REFERENCES mh_PlayersNew(PLAYER_ID)" + dataString + ", PRIMARY KEY(PLAYER_ID))"); //$NON-NLS-1$ //$NON-NLS-2$
		create.executeUpdate("CREATE TABLE IF NOT EXISTS mh_Achievements (PLAYER_ID INTEGER REFERENCES mh_PlayersNew(PLAYER_ID) NOT NULL, ACHIEVEMENT TEXT NOT NULL, DATE INTEGER NOT NULL, PROGRESS INTEGER NOT NULL, PRIMARY KEY(PLAYER_ID, ACHIEVEMENT), FOREIGN KEY(PLAYER_ID) REFERENCES mh_PlayersNew(PLAYER_ID))"); //$NON-NLS-1$
		
		setupTrigger(connection);
		
		create.close();
		
		connection.commit();
		
		performUUIDMigrate(connection);
		performAddNewMobs(connection);
	}

	private void setupTrigger(Connection connection) throws SQLException
	{
		
		Statement create = connection.createStatement();
		
		create.executeUpdate("create trigger if not exists mh_DailyInsert after insert on mh_Daily begin insert or ignore into mh_Weekly(ID, PLAYER_ID) values(strftime(\"%Y%W\",\"now\"), NEW.PLAYER_ID); insert or ignore into mh_Monthly(ID, PLAYER_ID) values(strftime(\"%Y%m\",\"now\"), NEW.PLAYER_ID); insert or ignore into mh_Yearly(ID, PLAYER_ID) values(strftime(\"%Y\",\"now\"), NEW.PLAYER_ID); insert or ignore into mh_AllTime(PLAYER_ID) values(NEW.PLAYER_ID); end"); //$NON-NLS-1$
		
		// Create the cascade update trigger. It will allow us to only modify the Daily table, and the rest will happen automatically
		StringBuilder updateStringBuilder = new StringBuilder();
		
		for(StatType type : StatType.values())
		{
			if(updateStringBuilder.length() != 0)
				updateStringBuilder.append(", "); //$NON-NLS-1$
			
			updateStringBuilder.append(String.format("%s = (%1$s + (NEW.%1$s - OLD.%1$s)) ", type.getDBColumn())); //$NON-NLS-1$
		}
		
		String updateString = updateStringBuilder.toString();
		
		StringBuilder updateTrigger = new StringBuilder();
		updateTrigger.append("create trigger if not exists mh_DailyUpdate after update on mh_Daily begin "); //$NON-NLS-1$
		
		// Weekly
		updateTrigger.append(" update mh_Weekly set "); //$NON-NLS-1$
		updateTrigger.append(updateString);
		updateTrigger.append(" where ID=strftime('%Y%W','now') AND PLAYER_ID=New.PLAYER_ID;"); //$NON-NLS-1$
		
		// Monthly
		updateTrigger.append(" update mh_Monthly set "); //$NON-NLS-1$
		updateTrigger.append(updateString);
		updateTrigger.append(" where ID=strftime('%Y%m','now') AND PLAYER_ID=New.PLAYER_ID;"); //$NON-NLS-1$
		
		// Yearly
		updateTrigger.append(" update mh_Yearly set "); //$NON-NLS-1$
		updateTrigger.append(updateString);
		updateTrigger.append(" where ID=strftime('%Y','now') AND PLAYER_ID=New.PLAYER_ID;"); //$NON-NLS-1$
		
		// AllTime
		updateTrigger.append(" update mh_AllTime set "); //$NON-NLS-1$
		updateTrigger.append(updateString);
		updateTrigger.append(" where PLAYER_ID=New.PLAYER_ID;"); //$NON-NLS-1$
		
		updateTrigger.append("END"); //$NON-NLS-1$
		
		create.executeUpdate(updateTrigger.toString());
		create.close();
		
		connection.commit();
	}

	@Override
	protected void setupStatements(Connection connection) throws SQLException
	{
		mAddPlayerStatement = connection.prepareStatement("INSERT OR IGNORE INTO mh_PlayersNew VALUES(?, ?, (SELECT IFNULL(MAX(PLAYER_ID),0)+1 FROM mh_PlayersNew));"); //$NON-NLS-1$
		mGetPlayerStatement[0] = connection.prepareStatement("SELECT * FROM mh_PlayersNew WHERE UUID=?;"); //$NON-NLS-1$
		mGetPlayerStatement[1] = connection.prepareStatement("SELECT * FROM mh_PlayersNew WHERE UUID IN (?,?);"); //$NON-NLS-1$
		mGetPlayerStatement[2] = connection.prepareStatement("SELECT * FROM mh_PlayersNew WHERE UUID IN (?,?,?,?,?);"); //$NON-NLS-1$
		mGetPlayerStatement[3] = connection.prepareStatement("SELECT * FROM mh_PlayersNew WHERE UUID IN (?,?,?,?,?,?,?,?,?,?);"); //$NON-NLS-1$
		
		mRecordAchievementStatement = connection.prepareStatement("INSERT OR REPLACE INTO mh_Achievements VALUES(?,?,?,?);"); //$NON-NLS-1$
		
		mAddPlayerStatsStatement = connection.prepareStatement("INSERT OR IGNORE INTO mh_Daily(ID, PLAYER_ID) VALUES(strftime(\"%Y%j\",\"now\"),?);"); //$NON-NLS-1$
		
		mLoadAchievementsStatement = connection.prepareStatement("SELECT ACHIEVEMENT, DATE, PROGRESS FROM mh_Achievements WHERE PLAYER_ID = ?;"); //$NON-NLS-1$
		
		mGetPlayerUUID = connection.prepareStatement("SELECT UUID FROM mh_PlayersNew WHERE NAME LIKE ?"); //$NON-NLS-1$
		mUpdatePlayerName = connection.prepareStatement("UPDATE mh_PlayersNew SET NAME=? WHERE UUID=?"); //$NON-NLS-1$
	}

	@Override
	public void saveStats( Set<StatStore> stats ) throws DataStoreException
	{
		try
		{
			Statement statement = mConnection.createStatement();
			
			HashSet<OfflinePlayer> names = new HashSet<OfflinePlayer>();
			for(StatStore stat : stats)
				names.add(stat.player);
			
			Map<UUID, Integer> ids = getPlayerIds(names);
			
			// Make sure the stats are available for each player
			mAddPlayerStatsStatement.clearBatch();
			for(OfflinePlayer player : names)
			{
				mAddPlayerStatsStatement.setInt(1, ids.get(player.getUniqueId()));
				mAddPlayerStatsStatement.addBatch();
			}

			mAddPlayerStatsStatement.executeBatch();
			
			// Now add each of the stats
			for(StatStore stat : stats)
				statement.addBatch(String.format("UPDATE mh_Daily SET %1$s = %1$s + %3$d WHERE ID = strftime(\"%%Y%%j\",\"now\") AND PLAYER_ID = %2$d;", stat.type.getDBColumn(), ids.get(stat.player.getUniqueId()), stat.amount)); //$NON-NLS-1$

			statement.executeBatch();
			
			statement.close();
			
			mConnection.commit();
		}
		catch(SQLException e)
		{
			rollback();
			throw new DataStoreException(e);
		}
	}
	
	@Override
	public List<StatStore> loadStats( StatType type, TimePeriod period, int count ) throws DataStoreException
	{
		try
		{
			String id;
			switch(period)
			{
			case Day:
				id = "strftime('%Y%j','now')"; //$NON-NLS-1$
				break;
			case Week:
				id = "strftime('%Y%W','now')"; //$NON-NLS-1$
				break;
			case Month:
				id = "strftime('%Y%m','now')"; //$NON-NLS-1$
				break;
			case Year:
				id = "strftime('%Y','now')"; //$NON-NLS-1$
				break;
			default:
				id = null;
				break;
			}
			
			Statement statement = mConnection.createStatement();
			ResultSet results = statement.executeQuery("SELECT " + type.getDBColumn() + ", mh_PlayersNew.UUID from " + period.getTable() + " inner join PlayersNew using (PLAYER_ID)" + (id != null ? " where ID=" + id : "") + " order by " + type.getDBColumn() + " desc limit " + count); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
			ArrayList<StatStore> list = new ArrayList<StatStore>();
			
			while(results.next())
				list.add(new StatStore(type, Bukkit.getOfflinePlayer(UUID.fromString(results.getString(2))), results.getInt(1)));
			
			results.close();
			return list;
		}
		catch(SQLException e)
		{
			throw new DataStoreException(e);
		}
	}
	
	private void performTableMigrate(Connection connection) throws SQLException
	{
		Statement statement = connection.createStatement();
		try
		{
			ResultSet rs = statement.executeQuery("SELECT * from mh_Players LIMIT 0");
			rs.close();

		}
		catch(SQLException e)
		{
			statement.close();
			return; // Tables will be fine
		}
		
		statement.executeUpdate("ALTER TABLE mh_Achievements RENAME TO mh_AchievementsOLD");
		statement.executeUpdate("ALTER TABLE mh_Daily RENAME TO mh_DailyOLD");
		statement.executeUpdate("ALTER TABLE mh_Weekly RENAME TO mh_WeeklyOLD");
		statement.executeUpdate("ALTER TABLE mh_Monthly RENAME TO mh_MonthlyOLD");
		statement.executeUpdate("ALTER TABLE mh_Yearly RENAME TO mh_YearlyOLD");
		statement.executeUpdate("ALTER TABLE mh_AllTime RENAME TO mh_AllTimeOLD");
		//statement.close();
	}
	
	private void finishTableMigrate(Statement statement) throws SQLException
	{
		//Statement statement = connection.createStatement();
		try
		{
			ResultSet rs = statement.executeQuery("SELECT * from mh_Players LIMIT 0");
			rs.close();

		}
		catch(SQLException e)
		{
			statement.close();
			return; // Tables will be fine
		}
		
		statement.executeUpdate("INSERT INTO mh_Achievements SELECT * FROM mh_AchievementsOLD");
		statement.executeUpdate("INSERT INTO mh_Daily SELECT * FROM mh_DailyOLD");
		statement.executeUpdate("INSERT INTO mh_Weekly SELECT * FROM mh_WeeklyOLD");
		statement.executeUpdate("INSERT INTO mh_Monthly SELECT * FROM mh_MonthlyOLD");
		statement.executeUpdate("INSERT INTO mh_Yearly SELECT * FROM mh_YearlyOLD");
		statement.executeUpdate("INSERT INTO mh_AllTime SELECT * FROM mh_AllTimeOLD");
		
		statement.executeUpdate("DROP TABLE mh_AchievementsOLD");
		statement.executeUpdate("DROP TABLE mh_DailyOLD");
		statement.executeUpdate("DROP TABLE mh_WeeklyOLD");
		statement.executeUpdate("DROP TABLE mh_MonthlyOLD");
		statement.executeUpdate("DROP TABLE mh_YearlyOLD");
		statement.executeUpdate("DROP TABLE mh_AllTimeOLD");
	}
	
	private void performUUIDMigrate(Connection connection) throws SQLException
	{
		Statement statement = connection.createStatement();
		try
		{
			ResultSet rs = statement.executeQuery("SELECT * from mh_Players LIMIT 0");
			rs.close();
		}
		catch(SQLException e)
		{
			statement.close();
			return; // UUIDs are in place
		}
		
		System.out.println("*** Migrating MobHunting Database to UUIDs ***");
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS mh_PlayersNew (UUID TEXT PRIMARY KEY, NAME TEXT, PLAYER_ID INTEGER NOT NULL)");
		
		ResultSet rs = statement.executeQuery("select `NAME`,`PLAYER_ID` from `mh_Players`");
		UUIDHelper.initialize();
		
		PreparedStatement insert = connection.prepareStatement("INSERT INTO mh_PlayersNew VALUES(?,?,?)");
		StringBuilder failString = new StringBuilder();
		int failCount = 0;
		while(rs.next())
		{
			String player = rs.getString(1);
			int pId = rs.getInt(2);
			UUID id = UUIDHelper.getKnown(player);
			if(id != null)
			{
				insert.setString(1, id.toString());
				insert.setString(2, player);
				insert.setInt(3, pId);
				insert.addBatch();
			}
			else
			{
				if(failString.length() != 0)
					failString.append(", ");
				failString.append(player);
				++failCount;
			}
		}
		
		rs.close();
		UUIDHelper.clearCache();
		
		if(failCount > 0)
		{
			System.err.println("* " + failCount + " accounts failed to convert:");
			System.err.println("** " + failString.toString());
		}
		
		insert.executeBatch();
		insert.close();
		
		statement.executeUpdate("drop table mh_PlayersNew");
		
		finishTableMigrate(statement);
		
		System.out.println("*** Player UUID migration complete ***");
		
		statement.close();
		connection.commit();
	}
	
	private void performAddNewMobs(Connection connection) throws SQLException
	{
		Statement statement = connection.createStatement();
		try
		{
			ResultSet rs = statement.executeQuery("SELECT Giant_kill from mh_Daily LIMIT 0");
			rs.close();
			statement.close();
			return; // New Mobs exists in database
		}
		catch(SQLException e)
		{
		}
		
		System.out.println("*** Adding new Mobs to MobHunting Database ***");

		statement.executeUpdate("alter table `mh_Daily` add column `Endermite_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Daily` add column `Endermite_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Weekly` add column `Endermite_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Weekly` add column `Endermite_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Monthly` add column `Endermite_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Monthly` add column `Endermite_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Yearly` add column `Endermite_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Yearly` add column `Endermite_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_AllTime` add column `Endermite_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_AllTime` add column `Endermite_assist`  INTEGER NOT NULL DEFAULT 0");
		
		statement.executeUpdate("alter table `mh_Daily` add column `Giant_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Daily` add column `Giant_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Weekly` add column `Giant_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Weekly` add column `Giant_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Monthly` add column `Giant_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Monthly` add column `Giant_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Yearly` add column `Giant_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Yearly` add column `Giant_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_AllTime` add column `Giant_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_AllTime` add column `Giant_assist`  INTEGER NOT NULL DEFAULT 0");

		statement.executeUpdate("alter table `mh_Daily` add column `Guardian_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Daily` add column `Guardian_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Weekly` add column `Guardian_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Weekly` add column `Guardian_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Monthly` add column `Guardian_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Monthly` add column `Guardian_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Yearly` add column `Guardian_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Yearly` add column `Guardian_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_AllTime` add column `Guardian_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_AllTime` add column `Guardian_assist`  INTEGER NOT NULL DEFAULT 0");

		statement.executeUpdate("alter table `mh_Daily` add column `KillerRabbit_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Daily` add column `KillerRabbit_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Weekly` add column `KillerRabbit_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Weekly` add column `KillerRabbit_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Monthly` add column `KillerRabbit_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Monthly` add column `KillerRabbit_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Yearly` add column `KillerRabbit_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_Yearly` add column `KillerRabbit_assist`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_AllTime` add column `KillerRabbit_kill`  INTEGER NOT NULL DEFAULT 0");
		statement.executeUpdate("alter table `mh_AllTime` add column `KillerRabbit_assist`  INTEGER NOT NULL DEFAULT 0");

		statement.executeUpdate("DROP TRIGGER IF EXISTS `mh_DailyInsert`");
		statement.executeUpdate("DROP TRIGGER IF EXISTS `mh_DailyUpdate`");
		setupTrigger(connection);
		
		System.out.println("*** Adding new Mobs complete ***");
		
		statement.close();
		connection.commit();
	}
}
