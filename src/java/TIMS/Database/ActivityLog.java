// Copyright (C) 2019 A*STAR
//
// TIMS (Translation Informatics Management System) is an software effort 
// by the ABSD (Analytics of Biological Sequence Data) team in the 
// Bioinformatics Institute (BII), Agency of Science, Technology and Research 
// (A*STAR), Singapore.
//

// This file is part of TIMS.
// 
// TIMS is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as 
// published by the Free Software Foundation, either version 3 of the 
// License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
package TIMS.Database;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ActivityLog implements Serializable {
    // activity_log table attributes
    private String user_id, activity, detail;
    private Timestamp now;
    private final static DateFormat df = 
            new SimpleDateFormat("dd-MMM-yyyy hh:mmaa");

    public ActivityLog(String user_id, String activity, String detail, 
            Timestamp now) {
        this.user_id = user_id;
        this.activity = activity;
        this.detail = detail;
        this.now = now;
    }

    // Return the timestamp in format "dd-MMM-yyyy hh:mmaa" for showing in 
    // the activity tracking page.
    public String getTimeString() {
        return df.format(now);
    }
    
    // Machine generated getters and setters.
    public String getUser_id() {
        return user_id;
    }
    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }
    public String getActivity() {
        return activity;
    }
    public void setActivity(String activity) {
        this.activity = activity;
    }
    public String getDetail() {
        return detail;
    }
    public void setDetail(String detail) {
        this.detail = detail;
    }
    public Timestamp getNow() {
        return now;
    }
    public void setNow(Timestamp now) {
        this.now = now;
    }
}
