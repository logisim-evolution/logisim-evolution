#!/sw/bin/tclsh
# -------------------------------------------------------------------------------
# HEIG-VD
#  Haute Ecole d'Ingenerie et de Gestion du Canton de Vaud
#  School of Business and Engineering in Canton de Vaud
# REDS Institute
#  Reconfigurable Embedded Digital Systems
# ------------------------------------------------------------------------------
# File         : questasim_binder.tcl
# Authors      : christian.mueller@heig-vd.ch (CMR)
# Date         : 19.05.2014
#
# Context      : This socket client is bound to Questsim/Modelsim simulation
#                and drives the signals received from Logisim. After a
#                simulation step, it returns the output signals to Logisim
#                through the socket.
#
#--| Modifications |------------------------------------------------------------
# Version   Author Date               Description
# v1.1      CMR    25.06.14           - Exit if socket fails
# v1.2      CMR    23.07.14           - Better error handling, add reset option
#-------------------------------------------------------------------------------
set Version 1.2

set msgs {}
set channel 0

proc MessageReceived {channel} {
	global msgs ;

	if {[eof $channel] || [catch {gets $channel msg}]} {
		end_binder $channel
  } else {

		# If end of communication is asked
		if {$msg == "end"} {

			# Sim end procedure
			end_binder $channel

		# When restart is requested
		} elseif {$msg == "restart"} {
			puts "Restart simulation"
			if {[catch {restart -f} errmsg]} {
				puts "Error at simulation reset: $errmsg"
			}
			set msgs {}

		# When a sync is received
		} elseif {$msg == "sync"} {

			# Drive input signals in simulation
			foreach msg $msgs {

				# Get parameters from message
				set signal [split $msg :]
				set type [lindex $signal 0]
				set name [lindex $signal 1]
				set value [lindex $signal 2]
				set id [lindex $signal 3]

				# If signal is "in"
				if {$type == 1} {
					# Drive simulation signal (force signal, no internal logic changes possible)
					if {[catch {force -freeze sim:/top_sim/$name $value} errmsg]} {
						puts "Error forcing simulation signal: $errmsg"
					}

				# If signal is "inout"
				} elseif {$type == 3} {

					# Use deposit so value can be changed by internal logic
					if {[catch {force -deposit sim:/top_sim/$name $value} errmsg]} {
						puts "Error forcing simulation signal: $errmsg"
					}
				}
			}

			# Run the simulation
			if {[catch {run 100} errmsg]} {
				puts "Error running simulation: $errmsg"
			}

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

					# Read in sim
					if {[catch {set value [examine sim:/top_sim/$name]} errmsg]} {
						puts "Error examining simulation signal: $errmsg"
					} else {
						# Send to logisim
						puts $channel [concat $name:$value:$id]
					}
				}
			}

			# Send sync to alert logisim of end of step
			puts $channel "sync"
			#puts "sent : sync"
			flush $channel

			set msgs {}

		# If it's a signal, add to list
		} else {
			lappend msgs $msg ;
		}
  }
}

proc {main} {port} {

	global argv
	global channel

	set server localhost

	if {[catch {set channel [socket $server $port]} errmsg]} {
		puts "Error : $errmsg"
		exit
	} else {
		puts "TCL_BINDER_CONNECTED"
		puts "TCL_BINDER_RUNNING"

		fileevent $channel readable [list MessageReceived $channel]

		vwait forever
	}
}

proc end_binder {channel} {

	if {[catch {quit -force} errmsg]} {
		puts "Error at simulator exit: $errmsg"
	}

	close $channel
	puts "TCL_BINDER_ENDED"
	exit
}

main $1
