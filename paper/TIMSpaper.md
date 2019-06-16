---
title: 'Translational Informatics Management System (TIMS): Towards OMICS based clinical data management for long term curation of clinical studies'
tags:
  - Data management
  - OMICS
authors:
  - name: Weihong Tay
    orcid: 0000-0000-0000-0000
    affiliation: 1
  - name: Erwin Tantoso
    orcid: 0000-0000-0000-0000
    affiliation: 1
  - name: Frank Eisenhaber
    orcid: 0000-0002-9599-5420
    affiliation: 1
  - name: Wing-Cheong Wong
    orcid: 0000-0003-1247-6279
    affiliation: 1
affiliations:
 - name: Bioinformatics Institute (BII), Agency for Science, Technology and Research  (A*STAR), 30 Biopolis Street, #07-01, Matrix, Singapore 138671
   index: 1
date: 14 June 2019
bibliography: TIMSpaper.bib
---

# Summary

With the maturation of sequencing technology over the past decade, the cost  associated to an OMICS based clinical study is no longer a limiting factor even for large cohorts e.g. the UK's 100K genomes project [@Samuel:2017]. However, the real cost of such study goes beyond sequencing or data generation in general [@Muir:2016]; The amount of raw sequencing data per sample can be quite sizable and quickly amass to quite a collection even for a modest cohort in contrast to the 
array based technology that it has inevitably displaced.

Often, a poorly tackled area in the post data production of cohort studies is the concerted management of the clinical meta-information (e.g., subjects' demographics, multiple records of domain specific clinical measures and other information) and the associated OMICS datasets over the course of these studies and eventually their long-term curation after their publication. In particular, these voluminous OMICS datasets require heavy preprocessing to obtain analysis-ready format (e.g., gene count quantification, genetic variants and mutations) prior to any phenotype genotype analysis. Another important consideration is the ability to re-process the OMICS datasets with alternative or updated algorithms where multiple datasets may be aggregated to perform analysis to test new hypotheses or to simply affirm the reproducibility [@Fanelli:2018] of the clinical results in a larger set. Although heavier systems (e.g. SysMO SEEK, DIPSBC, openBIS, Gaggle/BRM) do exist, they are not necessarily open source freeware and they often require complex deployment and distributed IT infrastructure [@Wruck:2014].

Specifically, we refer to an OMICS based clinical data management open-source software that curates study-related clinical information, manages the raw processing of diverse OMICS datasets to a preprocessed analysis ready state and finally visualizes the clinical information and processed output in a single access-controlled and audit trailed environment. Most importantly, this data management system should provide the skeletal  software framework for which any appropriate OMICS pipelines and visualizers can be integrated seamlessly in a scaleable fashion. For this purpose, the Translational Informatics Management System, herein TIMS software suite was built.

# Functionality and Implementation

The Translational Informatics Management System (TIMS) describes a server side clinical data management and OMICS production system for human research. From a technical overview, its central design is based on a Model-View-Controller (MVC) design pattern and implemented in Java server Faces (JSF) supplemented by general open source software packages as listed in Figure Table 1. In particular, GNU trove library and Ominfaces offer higher performance data structures (e.g. map, set, trees) and additional utilities than the native Java libraries. Meanwhile, the overall graphical user interface (GUI) is written in JSF with enhanced graphical modules from the PrimeFaces.

By functionality, TIMS implements 4 main functions : audit trail (activity tracking), access control (accounts/group management, study management),  workflow tracking (job management) and tools (pipeline, visualization); Refer to Figure 1. Firstly, the audit trail function captures all users' activities in the system which includes job submissions, data processing and any system activities. These information are pushed onto the backend relational database - PostgresSQL as the system's data store. Secondly, the access control function manages the users' access privilege based on a hierarchical structure of role/work unit (e.g. Director/Institute, Head/Department, Principal Investigator/Group, User/Group) through the accounts/group management module. Meanwhile, the study management module organizes both clinical and OMICS data into individualized cohort studies which are then assigned to users; All study data sets (both subjects' meta information and OMICS datasets) are stored in PostgresSQL database. Finally, the level of data access is dependent on the specific user's privilege set by the accounts/group management module. Thirdly, the workflow tracking function is implemented as a job management module to track data production (i.e., from raw data to pre processed/analysis ready data) of OMICs datasets. Any data production task submission via the appropriate OMICs pipeline will appear as a job under this module with their status (complete, on going) appropriately reflected. Lastly, the tools function contains tools for pipelines and visualizers. The current pipeline tools are OMICs centric and converts RAW data to its preprocessed format (see Figure Table 2 for complete list). Meanwhile, the current visualizer in TIMS - cBioPortal allows for visualization and query of the preprocessed OMICs data.

# Availability

The software is distributed under a GNU General Public License v3.0 is available at https://github.com/bii-absd/tims, together with the installation guide and tutorials. The demo datasets used in the tutorial are available for download at http://mendel.bii.a-star.edu.sg/SEQUENCES/TIMS/. A live demo version of TIMS is available at https://tims.bii.a-star.edu.sg/TIMS/login.xhtml.

# Figures

Figure 1: TIMS functionality, workflow and user/group structure ![](TIMS-figure.png)
Table Figure 1: Java libraries used in TIMS ![](TIMS-Table-Figure1.png)
Table Figure 2: OMICs pipeline tools available in TIMS ![](TIMS-Table-Figure2.png)

# Acknowledgements

We acknowledge contributions from Joanne Lee for the testing and documentation of TIMS during the genesis of this project.

# References

- `@Samuel et.al. 2017`  ->  "Samuel GN, Farsides B: The UK's 100,000 Genomes Project: manifesting policymakers' expectations. New Genet Soc 2017, 36:336-353."
- `@Muir et.al. 2016` -> "Muir P, Li S, Lou S, Wang D, Spakowicz DJ, Salichos L, Zhang J, Weinstock GM, Isaacs F, Rozowsky J et al.: The real cost of sequencing: scaling computation to keep pace with data generation. Genome Biol 2016, 17:53."
- `@Fanelli 2018` -> "Fanelli D: Opinion: Is science really facing a reproducibility crisis, and do we need it to? Proc Natl Acad Sci U S A 2018, 115:2628-2631."
- `@Wruck et.al. 2014` -> "Wruck W, Peuker M, Regenbrecht CR: Data management strategies for multinational large-scale systems biology projects. Brief Bioinform 2014, 15:65-78."