#  glue.tcl --
#
#      This file demonstrates how to load an external 
#      package and use the new commands.
#
#  Copyright (c) 1994-1997 Sun Microsystems, Inc.
#
#  See the file "license.terms" for information on usage and 
#  redistribution of this file, and for a DISCLAIMER OF ALL WARRANTIES.
#
#  RCS: @(#) $Id: glue.tcl,v 1.2 1998/11/04 23:43:39 hylands Exp $


# Load the package necessary to run the new commands.

java::load -classpath . GlueExtension


#  Main --
#  
#        Displays a filedialog that asks you to select a
#        jar file.  If a jar file is selected, display a
#        listing of the jar content.  Select one or more 
#        files from the listbox and the content of these
#        files will be displayed to stdout.
#  
#  Arguments:
#  
#        None.
#  
#  Results:
#  
#        The content of files within a jar file are 
#        printed to stdout.

proc Main { } {

    # It takes a second to create the AWT threads, let 
    # the users know whats going on.
    
    puts "Creating the AWT threads...\n"
    
    set jarFile [filedialog]

    set filesToExtract [SelectFilesToExtract $jarFile]

    set fileContent [ExtractFiles $jarFile $filesToExtract]

    for {set i 0} {$i < [llength $filesToExtract]} {incr i} {
	puts "\nfile \"[lindex $filesToExtract $i]\" content is:\n"
	puts [lindex $fileContent $i]
    }
    
    return
}


#  SelectFilesToExtract --
#  
#      If jarFile is a valid jar file, get a list 
#      of the contents.  Pass this list to listbox.
#      The listbox command returns a list of selected 
#      items (in this case files in the jar file).
#  
#  Arguments:
#  
#      jarFile    The name of a jar file.
#  
#  Results:
#  
#      Returns a list of files that are inside
#      the jarFile and were selected from the
#      listbox.

proc SelectFilesToExtract { jarFile } {

    if {$jarFile == {}} {
	return {}
    }

    # Calling the jar command with a jar filename
    # returns a list of files contained in the jar.

    set jarFileContent [jar $jarFile]

    # Passing a list of strings to list box will
    # display each list item.  The return of the
    # listbox command is another list of the 
    # selected items.

    set selectedFiles [eval listbox $jarFileContent]

    return $selectedFiles
}


#  ExtractFiles --
#  
#        Given a valid jar file and names of files 
#        within the jar file, extract the files and
#        return the content.
#  
#  Arguments:
#  
#        jarFile       name of jar file to extract files from
#        extractFiles  list of files to extract
#  
#  Results:
#  
#        The list of the extracted files content

proc ExtractFiles { jarFile extractFiles } {

    if {$jarFile == {} || $extractFiles == {}} {
	return {}
    }

    set content {}

    # Calling the jar command with the '-extract' option will
    # return the content of the file within the jar.

    for {set i 0} {$i < [llength $extractFiles]} {incr i} {
	lappend content [jar $jarFile -extract [lindex $extractFiles $i]]
    }

    return $content
}


# Call Main to get the ball rolling...

Main
