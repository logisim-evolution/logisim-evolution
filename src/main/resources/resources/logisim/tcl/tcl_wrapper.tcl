#!/sw/bin/tclsh
# -------------------------------------------------------------------------------
# HEIG-VD
#  Haute Ecole d'Ingenerie et de Gestion du Canton de Vaud
#  School of Business and Engineering in Canton de Vaud
# REDS Institute
#  Reconfigurable Embedded Digital Systems
# ------------------------------------------------------------------------------
# File         : tcl_wrapper.tcl
# Authors      : christian.mueller@heig-vd.ch (CMR)
# Date         : 20.08.2014
#
# Context      : This socket client binds Logisim to a TCL script. It gets his
#								 input from the socket binded to Logisim, computes the output
#								 and send it back through the socket.
#
#--| Modifications |------------------------------------------------------------
# Version   Author Date               Description
# v1.0      CMR 20.08.2014  Original, copied from questasim_binder
# v1.1      YSR 26.06.2015  Modifications to support and fix issues with sequential systems
#-------------------------------------------------------------------------------
set Version 1.1

set channel 0
set msgs {}



proc examine {args} {
	set arguments [split $args " "]

	switch [llength $arguments] {
         1 {return [examine1 [lindex $arguments 0]]}
         2 {return [examine2 [lindex $arguments 0] [lindex $arguments 1]]}
         default {echo "Error, wrong number of args for examine function : examine (type) signal. You called \"examine $args\""}
    }

    return X
}

proc examine1 {signal} {

	set signal_name [getName $signal]
	set signal_index [getIndex $signal]

	global $signal_name

	if {$signal_index == ""} {

		if {[catch {set signal_value [set $signal_name]} errmsg]} {
			return 0
		}

		#echo "Examining signal $signal_name:$signal_value"
		return $signal_value
	}	else {
		echo "Single bit read is not implemented"
		return 0

		#echo "Value is : [string index [set $signal_name] signal_index]"
		#return [string index [set $signal_name] signal_index]
	}
}

proc examine2 {type signal} {
	return examine1 $signal
}



proc force {args} {
	set arguments [split $args " "]

	switch [llength $arguments] {
         2 {force2 [lindex $arguments 0] [lindex $arguments 1]}
         3 {force3 [lindex $arguments 0] [lindex $arguments 1] [lindex $arguments 2]}
         default {echo "Error, wrong number of args for force function : force (type) signal value. You called \"force $args\""}
    }
}

proc force2 {signal value} {
	set signal_name [getName $signal]

	global $signal_name

	#echo "Forcing signal $signal_name to $value"
	set $signal_name $value

	# Asynchronous not supported
	#send_socket $channel "[getName $signal]:$value"
}

proc force3 {type signal value} {
	force2 $signal $value
}




proc run {args} {
	global channel

	send_socket $channel "run"
}

proc restart {args} {
	echo "Restart"
}

proc add {args} {
	echo "'add' command ignored in Logisim execution mode"
}

proc delete {args} {
	echo "'delete' command ignored in Logisim execution mode"
}

proc configure {args} {
	echo "'configure' command ignored in Logisim execution mode"
}

proc WaveRestoreZoom {args} {
	echo "'WaveRestoreZoom' command ignored in Logisim execution mode"
}

proc getName {signal} {
	regexp {([^\/\(]+)[0-9\(\)]*$} $signal matched signal_name
	return $signal_name
}

proc getIndex {signal} {
	regexp {[^\/\(]+\(?([0-9]*)\)?$} $signal matched index
	return $index
}

proc MessageReceived {channel} {
	global msgs

	if {[eof $channel] || [catch {gets $channel msg}]} {
		end_binder $channel
  } else {

		# If end of communication is asked
		if {$msg == "end"} {

			# Sim end procedure
			end_binder $channel

		# When a sync_force is received
		} elseif {$msg == "sync_force"} {
                        #echo "Recieved sync_force!"

			# Drive input signals in simulation
			foreach msg $msgs {

				# Get parameters from message
				set signal [split $msg :]
				set type [lindex $signal 0]
				set name [lindex $signal 1]
				set value [lindex $signal 2]
				set id [lindex $signal 3]

				global $name
				# If signal is "in" or "inout"
				if {$type == 1 || $type == 3} {
					#echo "Received from Logsim : $name:$value"
					set $name $value
				} else {
					set $name X
				}
			}

			logisimForce

			# Read output signals from simulation
			foreach msg $msgs {

				# Get signal and value from message
				set signal [split $msg :]
				set type [lindex $signal 0]
				set name [lindex $signal 1]
				set value [lindex $signal 2]
				set id [lindex $signal 3]


				# If signal is "out" or "inout"
				if {$type == 2 || $type == 3} {
					#echo "Send [concat $name:[set $name]:$id] to logisim"
					# Send to logisim
					send_socket $channel [concat $name:[set $name]:$id]
				}
			}

			# Send sync to alert logisim of end of step
			#echo "Send : sync"
			send_socket $channel "sync"

			set msgs {}

		# When a sync_examine is received
		} elseif {$msg == "sync_examine"} {
                        #echo "Recieved sync_examine!"

			# Drive input signals in simulation
			foreach msg $msgs {

				# Get parameters from message
				set signal [split $msg :]
				set type [lindex $signal 0]
				set name [lindex $signal 1]
				set value [lindex $signal 2]
				set id [lindex $signal 3]

				global $name
				# If signal is "in" or "inout"
				if {$type == 1 || $type == 3} {
					#echo "Received from Logsim : $name:$value"
					set $name $value
				} else {
					set $name X
				}
			}

			logisimExamine

			# Send sync to alert logisim of end of step
			#echo "Send : sync"
			send_socket $channel "sync"

			set msgs {}

		# If it's a signal, add to list
		} else {
			lappend msgs $msg ;
		}
  }
}

proc end_binder {channel} {

	close $channel
	puts "TCL_WRAPPER_ENDED"
	exit
}

proc init_wrapper {} {

	global msgs
	global argv
	global channel

        enableLogisim TRUE

	set server localhost
	set port [lindex $argv 0]

	if {[catch {set channel [socket $server $port]} errmsg]} {
		puts "Error : $errmsg"
		exit
	} else {
		puts "TCL_WRAPPER_CONNECTED"
		puts "TCL_WRAPPER_RUNNING"
	}

	# fconfigure $channel -buffering line
	fileevent $channel readable [list MessageReceived $channel]

}

proc send_socket {channel message} {
	puts $channel $message
	flush $channel
}

proc echo {message} {
	puts "\[TCL\] $message"
}

# Source the TCL script file
source [lindex $argv 1]

# Init this wrapper
init_wrapper
