# configuration script for the MLN query & parameter learning tools

import os

# Note:
#   Most settings need not be changed to use PyMLNs.
#   Depending on your naming convention for mln and database files, you may, however need to change
#      query_db_filemask, query_mln_filemask, learnwts_mln_filemask and learnwts_db_filemask
#   to suit your need.
#   You can use os.getenv("VARIABLE") to retrieve the value of an environment variable

# --- settings for both tools ---

fixed_width_font = ("Lucida Console", -12) # name of font and size (if negative, in pixels)
editor = os.getenv("EDITOR") # your favorite editor with which output files should be opened, e.g. "kate" or "vi"


# --- settings for the parameter learning tool ---

learnwts_mln_filemask = "*.mln"
learnwts_db_filemask = "*.db"
def learnwts_output_filename(infile, engine, method, dbfile): # formats the output filename
    if infile[:3] == "in.": infile = infile[3:]
    elif infile[:4] == "wts.": infile = infile[4:]
    if infile[-4:] == ".mln": infile = infile[:-4]
    if dbfile[-3:] == ".db": dbfile = dbfile[:-3]
    return "wts.%s%s.%s-%s.mln" % (engine, method, dbfile, infile)
learnwts_full_report = True # if True, add all the printed output to the Alchemy output file, otherwise (False) use a short report
learnwts_report_bottom = True # if True, the comment with the report is appended to the end of the file, otherwise it is inserted at the beginning


#  --- settings for the query tool ---

query_mln_filemask = "*.mln"
query_db_filemask = ["*.db", "*.blogdb"]
def query_output_filename(mlnfile, dbfile):
    if mlnfile[:4] == "wts.": mlnfile = mlnfile[4:]
    if mlnfile[-4:] == ".mln": mlnfile = mlnfile[:-4]
    if dbfile[-3:] == ".db": dbfile = dbfile[:-3]
    return "%s-%s.results" % (dbfile, mlnfile)
query_edit_outfile_when_done = False # if True, open the output file that is generated by the Alchemy system in the editor defined above
keep_alchemy_conversions = True

# --- Alchemy settings ---

# define how the Alchemy system is to be used, i.e. what certain command line switches are
old_usage = {
    "openWorld": "-o",
    "maxSteps": "-mcmcMaxSteps",
    "numChains": "-mcmcNumChains",
}
new_usage = {
    "openWorld": "-ow",
    "maxSteps": "-maxSteps",
    "numChains": "-numChains"
}
default_infer_usage = old_usage # the usage that is to apply when the "usage" of an Alchemy installation is not set explicitly in the dictionary below

# installed Alchemy versions:
# - Keys are names of the installations as they should appear in the two tools.
# - Values should be either paths to the Alchemy root or "bin" directory or 
#   a dictionary with at least the key "path" set to the Alchemy root or bin directory.
#   The dictionary can additionally set "usage" to one of the above mappings
alchemy_versions = {#"Alchemy - current (AMD64)": {"path": os.getenv("ALCHEMY_HOME"), "usage": new_usage},
                    "Alchemy - January 2007 (AMD64)": os.getenv("ALCHEMY_JAN07"),
                    "Alchemy - June 2008 (AMD64)": {"path": "/usr/wiss/jain/work/code/alchemy-2008-06-30/bin/amd64", "usage": new_usage},
                    "Alchemy - June 2008 (i386)": {"path": "/usr/wiss/jain/work/code/alchemy-2008-06-30/bin/i386", "usage": new_usage},
                    "Alchemy - January 2007 (i386)": os.getenv("ALCHEMY_JAN07_32"),
                    "Alchemy - January 2007/dev (i386)": os.getenv("ALCHEMY_JAN07_32")+"_dev",
		    "Alchemy - February 2008 (i386)": {"path": "/usr/wiss/jain/work/code/alchemy-2008-02-07/bin/i386", "usage": new_usage}
                    #"dev" : os.getenv("ALCHEMY_HOME_DEV"),
                    #"old" : os.getenv("ALCHEMY_HOME_OLD"),
                    #"snapshot dev" : os.getenv("ALCHEMY_HOME_NEW"),
                    #"snapshot" : os.path.join(os.getenv("ALCHEMY_HOME_NEW"), "bin", "stable"),
                    #"snapshot original" : r"C:\Dev\C++\alchemy\alchemy-snapshot",
                    #"snapshot learnwts-startpoint" : os.path.join(os.getenv("ALCHEMY_HOME_NEW"), "bin", "stable-startpt")
                   }
'''
# snapshot, snapshot original and (if the weights in the input MLN are all 0) all yield the same results when weight learning
# snapshot is a lot faster than snapshot original though
# snapshot learnwts-startpnt modifies the start point of the optimization, using the weights given in the mln (if any) instead of a zero vector - these weights are not used as priors for penalizing though (reset to 0)
# snapshot dev has a different counting method (which I think is correct - unlike the original one)
'''
