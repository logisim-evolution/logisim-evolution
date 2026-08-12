library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

entity testbench is
end testbench;

architecture sim of testbench is
signal A, B
signal OR_out
signal XOR_out
signal AND_out

component compuertas
Port (

A

B

OR_out : out STD_LOGIC;
XOR_out : out STD_LOGIC;
AND_out : out STD_LOGIC

end component;

begin
UUT: compuertas port map (
A

B

: STD_LOGIC := '0';
: STD_LOGIC;
: STD_LOGIC;
: STD_LOGIC;

: in STD_LOGIC;
: in STD_LOGIC;

);

=> A,

=> B,
