--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'SQL_ASCII';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

SET default_with_oids = false;

--
-- Name: activity_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE activity_log (
    sn integer NOT NULL,
    user_id character varying(20),
    activity text,
    detail text,
    "time" timestamp without time zone
);


--
-- Name: activity_log_sn_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE activity_log_sn_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: activity_log_sn_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE activity_log_sn_seq OWNED BY activity_log.sn;


--
-- Name: annotation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE annotation (
    annot_ver text NOT NULL,
    date date NOT NULL
);


--
-- Name: dashboard_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE dashboard_config (
    chart_id text NOT NULL,
    study_id text NOT NULL,
    title text,
    data_source_x text,
    data_source_y text,
    label_x text
);


--
-- Name: data_depository; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE data_depository (
    genename text NOT NULL,
    annot_ver text NOT NULL,
    data text[]
);


--
-- Name: dept; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE dept (
    dept_id character varying(10) NOT NULL,
    inst_id character varying(10) NOT NULL,
    dept_name text NOT NULL
);


--
-- Name: feature; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE feature (
    fcode text NOT NULL,
    status text,
    options text
);


--
-- Name: finalized_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE finalized_record (
    array_index integer NOT NULL,
    annot_ver text NOT NULL,
    job_id integer NOT NULL,
    subject_id text NOT NULL,
    study_id text NOT NULL
);


--
-- Name: generef; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE generef (
    genename text NOT NULL,
    name text NOT NULL,
    chrom text NOT NULL,
    strand character(1) NOT NULL,
    txstart integer NOT NULL,
    txend integer NOT NULL,
    cdsstart integer NOT NULL,
    cdsend integer NOT NULL,
    exoncount smallint NOT NULL,
    exonstarts text NOT NULL,
    exonends text NOT NULL,
    annot_ver text NOT NULL
);


--
-- Name: grp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE grp (
    grp_id character varying(10) NOT NULL,
    dept_id character varying(10) NOT NULL,
    grp_name text NOT NULL,
    pi character varying(20),
    active boolean DEFAULT true
);


--
-- Name: icd; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE icd (
    icd_code character varying(3) NOT NULL,
    icd_desc text
);


--
-- Name: input_data; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE input_data (
    sn integer NOT NULL,
    study_id text NOT NULL,
    create_uid character varying(20),
    update_uid character varying(20),
    pipeline_name text,
    filename text,
    filepath text NOT NULL,
    description text,
    create_time timestamp without time zone,
    update_time timestamp without time zone
);


--
-- Name: inst; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE inst (
    inst_id character varying(10) NOT NULL,
    inst_name text NOT NULL
);


--
-- Name: inst_dept_grp; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW inst_dept_grp AS
 SELECT id.inst_id,
    id.inst_name,
    id.dept_id,
    id.dept_name,
    g.grp_id,
    g.grp_name,
    g.pi,
    g.active
   FROM (( SELECT grp.grp_id,
            grp.dept_id,
            grp.grp_name,
            grp.pi,
            grp.active
           FROM grp) g
     JOIN ( SELECT inst.inst_id,
            inst.inst_name,
            dept.dept_id,
            dept.dept_name
           FROM (inst
             JOIN dept USING (inst_id))) id USING (dept_id));


--
-- Name: job_status; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE job_status (
    status_id integer NOT NULL,
    status_name text NOT NULL
);


--
-- Name: job_status_status_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE job_status_status_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: job_status_status_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE job_status_status_id_seq OWNED BY job_status.status_id;


--
-- Name: kgxref; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE kgxref (
    kgid text NOT NULL,
    mrna text NOT NULL,
    spid text NOT NULL,
    spdisplayid text NOT NULL,
    genesymbol text NOT NULL,
    refseq text NOT NULL,
    protacc text NOT NULL,
    description text NOT NULL,
    rfamacc text NOT NULL,
    trnaname text NOT NULL,
    canonical boolean,
    annot_ver text NOT NULL
);


--
-- Name: meta_data_tag; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE meta_data_tag (
    core_data text NOT NULL,
    study_id text NOT NULL,
    column_id text
);


--
-- Name: nationality; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE nationality (
    country_code character varying(3) NOT NULL,
    country_name text
);


--
-- Name: pipeline; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE pipeline (
    name text NOT NULL,
    description text NOT NULL,
    tid character varying(20),
    command text NOT NULL,
    parameter text,
    editable boolean DEFAULT false
);


--
-- Name: pipeline_technology; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE pipeline_technology (
    tid character varying(20) NOT NULL,
    description text
);


--
-- Name: pipeline_visualiser; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE pipeline_visualiser (
    pipeline_name text NOT NULL,
    vpid text NOT NULL
);


--
-- Name: study; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE study (
    study_id text NOT NULL,
    grp_id character varying(10) NOT NULL,
    annot_ver text NOT NULL,
    icd_code character varying(3) NOT NULL,
    title text NOT NULL,
    description text NOT NULL,
    background text,
    grant_info text,
    start_date date,
    end_date date,
    finalized_output text,
    detail_files text,
    summary text,
    cbio_url text,
    visual_time timestamp without time zone,
    finalized boolean,
    closed boolean,
    data_col_name_list bytea,
    meta_quality_report text
);


--
-- Name: study_specific_fields; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE study_specific_fields (
    id integer NOT NULL,
    category text NOT NULL,
    study_id text NOT NULL,
    fields bytea
);


--
-- Name: study_specific_fields_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE study_specific_fields_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: study_specific_fields_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE study_specific_fields_id_seq OWNED BY study_specific_fields.id;


--
-- Name: subject; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE subject (
    subject_id text NOT NULL,
    study_id text NOT NULL,
    gender text,
    race text,
    dob date,
    casecontrol text,
    age_at_baseline text
);


--
-- Name: subject_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE subject_record (
    subject_id text NOT NULL,
    study_id text NOT NULL,
    record_date date NOT NULL,
    height text,
    weight text,
    sample_id text,
    dat bytea
);


--
-- Name: study_subject_output; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW study_subject_output AS
 SELECT x.study_id,
    x.subject_id,
    y.annot_ver,
    y.icd_code,
    y.array_index,
    x.casecontrol,
    x.record_date,
    x.gender,
    x.race,
    x.dob,
    x.age_at_baseline,
    x.height,
    x.weight,
    x.sample_id
   FROM (( SELECT s.subject_id,
            s.study_id,
            s.gender,
            s.race,
            s.dob,
            s.casecontrol,
            s.age_at_baseline,
            sr.record_date,
            sr.height,
            sr.weight,
            sr.sample_id,
            sr.dat
           FROM (subject s
             JOIN subject_record sr USING (subject_id, study_id))) x
     JOIN ( SELECT fr.annot_ver,
            fr.study_id,
            fr.array_index,
            fr.job_id,
            fr.subject_id,
            st.grp_id,
            st.icd_code,
            st.title,
            st.description,
            st.background,
            st.grant_info,
            st.start_date,
            st.end_date,
            st.finalized_output,
            st.detail_files,
            st.summary,
            st.cbio_url,
            st.visual_time,
            st.finalized,
            st.closed,
            st.data_col_name_list,
            st.meta_quality_report
           FROM (finalized_record fr
             JOIN study st USING (annot_ver, study_id))) y ON (((x.study_id = y.study_id) AND (x.subject_id = y.subject_id))));


--
-- Name: subject_detail; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW subject_detail AS
 SELECT sr.study_id,
    sr.subject_id,
    sr.record_date,
    s.gender,
    s.race,
    s.dob,
    s.casecontrol,
    s.age_at_baseline,
    sr.height,
    sr.weight,
    sr.sample_id,
    sr.dat
   FROM (subject s
     JOIN subject_record sr USING (subject_id, study_id))
  ORDER BY sr.study_id, sr.subject_id, sr.record_date;


--
-- Name: submitted_job; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE submitted_job (
    job_id integer NOT NULL,
    study_id text NOT NULL,
    user_id character varying(20) NOT NULL,
    pipeline_name text NOT NULL,
    status_id integer NOT NULL,
    submit_time timestamp without time zone NOT NULL,
    complete_time timestamp without time zone,
    input_sn integer NOT NULL,
    input_desc text NOT NULL,
    parameters text,
    output_file text,
    detail_output text,
    report text,
    cbio_target boolean DEFAULT false
);


--
-- Name: submitted_job_job_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE submitted_job_job_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: submitted_job_job_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE submitted_job_job_id_seq OWNED BY submitted_job.job_id;


--
-- Name: system_parameters; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE system_parameters (
    sys_para_name text NOT NULL,
    sys_para_value text
);


--
-- Name: user_account; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE user_account (
    user_id character varying(20) NOT NULL,
    role_id integer NOT NULL,
    unit_id character varying(10) NOT NULL,
    first_name text NOT NULL,
    last_name text NOT NULL,
    photo text,
    email text NOT NULL,
    pwd character varying(60) NOT NULL,
    active boolean NOT NULL,
    last_login text
);


--
-- Name: user_role; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE user_role (
    role_id integer NOT NULL,
    role_name text NOT NULL
);


--
-- Name: user_role_role_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE user_role_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_role_role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE user_role_role_id_seq OWNED BY user_role.role_id;


--
-- Name: vault_data; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE vault_data (
    genename text NOT NULL,
    annot_ver text NOT NULL,
    data text[]
);


--
-- Name: vault_record; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE vault_record (
    array_index integer NOT NULL,
    annot_ver text NOT NULL,
    job_id integer NOT NULL,
    subject_id text NOT NULL,
    study_id text NOT NULL
);


--
-- Name: visual_profile; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE visual_profile (
    vpid text NOT NULL,
    vname text NOT NULL,
    profile text NOT NULL,
    description text
);


--
-- Name: visual_profile_detail; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW visual_profile_detail AS
 SELECT x.vname,
    x.profile,
    y.pipeline_name,
    y.vpid,
    x.description
   FROM (visual_profile x
     JOIN pipeline_visualiser y ON ((x.vpid = y.vpid)));


--
-- Name: sn; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY activity_log ALTER COLUMN sn SET DEFAULT nextval('activity_log_sn_seq'::regclass);


--
-- Name: status_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY job_status ALTER COLUMN status_id SET DEFAULT nextval('job_status_status_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY study_specific_fields ALTER COLUMN id SET DEFAULT nextval('study_specific_fields_id_seq'::regclass);


--
-- Name: job_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY submitted_job ALTER COLUMN job_id SET DEFAULT nextval('submitted_job_job_id_seq'::regclass);


--
-- Name: role_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY user_role ALTER COLUMN role_id SET DEFAULT nextval('user_role_role_id_seq'::regclass);


--
-- Name: activity_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY activity_log
    ADD CONSTRAINT activity_log_pkey PRIMARY KEY (sn);


--
-- Name: annotation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY annotation
    ADD CONSTRAINT annotation_pkey PRIMARY KEY (annot_ver);


--
-- Name: dashboard_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY dashboard_config
    ADD CONSTRAINT dashboard_config_pkey PRIMARY KEY (study_id, chart_id);


--
-- Name: data_depository_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY data_depository
    ADD CONSTRAINT data_depository_pkey PRIMARY KEY (genename, annot_ver);


--
-- Name: dept_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY dept
    ADD CONSTRAINT dept_pkey PRIMARY KEY (dept_id);


--
-- Name: feature_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY feature
    ADD CONSTRAINT feature_pkey PRIMARY KEY (fcode);


--
-- Name: finalized_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY finalized_record
    ADD CONSTRAINT finalized_record_pkey PRIMARY KEY (array_index, annot_ver);


--
-- Name: generef_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY generef
    ADD CONSTRAINT generef_pkey PRIMARY KEY (name, annot_ver, txstart);


--
-- Name: grp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY grp
    ADD CONSTRAINT grp_pkey PRIMARY KEY (grp_id);


--
-- Name: icd_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY icd
    ADD CONSTRAINT icd_pkey PRIMARY KEY (icd_code);


--
-- Name: input_data_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY input_data
    ADD CONSTRAINT input_data_pkey PRIMARY KEY (sn, study_id);


--
-- Name: inst_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY inst
    ADD CONSTRAINT inst_pkey PRIMARY KEY (inst_id);


--
-- Name: job_status_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY job_status
    ADD CONSTRAINT job_status_pkey PRIMARY KEY (status_id);


--
-- Name: job_status_status_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY job_status
    ADD CONSTRAINT job_status_status_name_key UNIQUE (status_name);


--
-- Name: kgxref_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY kgxref
    ADD CONSTRAINT kgxref_pkey PRIMARY KEY (kgid, annot_ver);


--
-- Name: meta_data_tag_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY meta_data_tag
    ADD CONSTRAINT meta_data_tag_pkey PRIMARY KEY (study_id, core_data);


--
-- Name: nationality_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY nationality
    ADD CONSTRAINT nationality_pkey PRIMARY KEY (country_code);


--
-- Name: pipeline_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY pipeline
    ADD CONSTRAINT pipeline_pkey PRIMARY KEY (name);


--
-- Name: pipeline_technology_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY pipeline_technology
    ADD CONSTRAINT pipeline_technology_pkey PRIMARY KEY (tid);


--
-- Name: pipeline_visualiser_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY pipeline_visualiser
    ADD CONSTRAINT pipeline_visualiser_pkey PRIMARY KEY (pipeline_name, vpid);


--
-- Name: study_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY study
    ADD CONSTRAINT study_pkey PRIMARY KEY (study_id);


--
-- Name: study_specific_fields_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY study_specific_fields
    ADD CONSTRAINT study_specific_fields_pkey PRIMARY KEY (study_id, category);


--
-- Name: subject_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY subject
    ADD CONSTRAINT subject_pkey PRIMARY KEY (study_id, subject_id);


--
-- Name: subject_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY subject_record
    ADD CONSTRAINT subject_record_pkey PRIMARY KEY (study_id, subject_id, record_date);


--
-- Name: submitted_job_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY submitted_job
    ADD CONSTRAINT submitted_job_pkey PRIMARY KEY (job_id);


--
-- Name: system_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY system_parameters
    ADD CONSTRAINT system_parameters_pkey PRIMARY KEY (sys_para_name);


--
-- Name: user_account_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY user_account
    ADD CONSTRAINT user_account_pkey PRIMARY KEY (user_id);


--
-- Name: user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY user_role
    ADD CONSTRAINT user_role_pkey PRIMARY KEY (role_id);


--
-- Name: user_role_role_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY user_role
    ADD CONSTRAINT user_role_role_name_key UNIQUE (role_name);


--
-- Name: vault_data_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY vault_data
    ADD CONSTRAINT vault_data_pkey PRIMARY KEY (genename, annot_ver);


--
-- Name: vault_record_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY vault_record
    ADD CONSTRAINT vault_record_pkey PRIMARY KEY (array_index, annot_ver);


--
-- Name: visual_profile_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY visual_profile
    ADD CONSTRAINT visual_profile_pkey PRIMARY KEY (vpid);


--
-- Name: deposit_annot_ind; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX deposit_annot_ind ON data_depository USING btree (annot_ver);


--
-- Name: vault_annot_ind; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX vault_annot_ind ON vault_data USING btree (annot_ver);


--
-- Name: data_depository_annot_ver_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY data_depository
    ADD CONSTRAINT data_depository_annot_ver_fkey FOREIGN KEY (annot_ver) REFERENCES annotation(annot_ver);


--
-- Name: db_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY dashboard_config
    ADD CONSTRAINT db_study_id_fkey FOREIGN KEY (study_id) REFERENCES study(study_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: dept_inst_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY dept
    ADD CONSTRAINT dept_inst_id_fkey FOREIGN KEY (inst_id) REFERENCES inst(inst_id);


--
-- Name: finalized_output_annot_ver_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY finalized_record
    ADD CONSTRAINT finalized_output_annot_ver_fkey FOREIGN KEY (annot_ver) REFERENCES annotation(annot_ver);


--
-- Name: generef_version_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY generef
    ADD CONSTRAINT generef_version_fkey FOREIGN KEY (annot_ver) REFERENCES annotation(annot_ver);


--
-- Name: grp_dept_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY grp
    ADD CONSTRAINT grp_dept_id_fkey FOREIGN KEY (dept_id) REFERENCES dept(dept_id);


--
-- Name: grp_pi_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY grp
    ADD CONSTRAINT grp_pi_fkey FOREIGN KEY (pi) REFERENCES user_account(user_id);


--
-- Name: input_data_create_uid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY input_data
    ADD CONSTRAINT input_data_create_uid_fkey FOREIGN KEY (create_uid) REFERENCES user_account(user_id);


--
-- Name: input_data_pipeline_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY input_data
    ADD CONSTRAINT input_data_pipeline_name_fkey FOREIGN KEY (pipeline_name) REFERENCES pipeline(name) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: input_data_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY input_data
    ADD CONSTRAINT input_data_study_id_fkey FOREIGN KEY (study_id) REFERENCES study(study_id);


--
-- Name: input_data_update_uid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY input_data
    ADD CONSTRAINT input_data_update_uid_fkey FOREIGN KEY (update_uid) REFERENCES user_account(user_id);


--
-- Name: kgxref_version_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY kgxref
    ADD CONSTRAINT kgxref_version_fkey FOREIGN KEY (annot_ver) REFERENCES annotation(annot_ver);


--
-- Name: mdt_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY meta_data_tag
    ADD CONSTRAINT mdt_study_id_fkey FOREIGN KEY (study_id) REFERENCES study(study_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: pipeline_tid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY pipeline
    ADD CONSTRAINT pipeline_tid_fkey FOREIGN KEY (tid) REFERENCES pipeline_technology(tid) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: pipeline_visualiser_pipeline_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY pipeline_visualiser
    ADD CONSTRAINT pipeline_visualiser_pipeline_name_fkey FOREIGN KEY (pipeline_name) REFERENCES pipeline(name) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: pipeline_visualiser_vpid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY pipeline_visualiser
    ADD CONSTRAINT pipeline_visualiser_vpid_fkey FOREIGN KEY (vpid) REFERENCES visual_profile(vpid) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: ssf_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY study_specific_fields
    ADD CONSTRAINT ssf_study_id_fkey FOREIGN KEY (study_id) REFERENCES study(study_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: study_annot_ver_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY study
    ADD CONSTRAINT study_annot_ver_fkey FOREIGN KEY (annot_ver) REFERENCES annotation(annot_ver);


--
-- Name: study_grp_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY study
    ADD CONSTRAINT study_grp_id_fkey FOREIGN KEY (grp_id) REFERENCES grp(grp_id);


--
-- Name: study_icd_code_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY study
    ADD CONSTRAINT study_icd_code_fkey FOREIGN KEY (icd_code) REFERENCES icd(icd_code);


--
-- Name: subject_foreign_keys; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY subject_record
    ADD CONSTRAINT subject_foreign_keys FOREIGN KEY (study_id, subject_id) REFERENCES subject(study_id, subject_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: subject_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY subject
    ADD CONSTRAINT subject_study_id_fkey FOREIGN KEY (study_id) REFERENCES study(study_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: submitted_job_pipeline_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY submitted_job
    ADD CONSTRAINT submitted_job_pipeline_name_fkey FOREIGN KEY (pipeline_name) REFERENCES pipeline(name) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: submitted_job_status_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY submitted_job
    ADD CONSTRAINT submitted_job_status_id_fkey FOREIGN KEY (status_id) REFERENCES job_status(status_id);


--
-- Name: submitted_job_study_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY submitted_job
    ADD CONSTRAINT submitted_job_study_id_fkey FOREIGN KEY (study_id) REFERENCES study(study_id);


--
-- Name: submitted_job_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY submitted_job
    ADD CONSTRAINT submitted_job_user_id_fkey FOREIGN KEY (user_id) REFERENCES user_account(user_id);


--
-- Name: user_account_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY user_account
    ADD CONSTRAINT user_account_role_id_fkey FOREIGN KEY (role_id) REFERENCES user_role(role_id);


--
-- Name: vault_data_annot_ver_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY vault_data
    ADD CONSTRAINT vault_data_annot_ver_fkey FOREIGN KEY (annot_ver) REFERENCES annotation(annot_ver);


--
-- Name: vault_record_annot_ver_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY vault_record
    ADD CONSTRAINT vault_record_annot_ver_fkey FOREIGN KEY (annot_ver) REFERENCES annotation(annot_ver);


--
-- Name: public; Type: ACL; Schema: -; Owner: -
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

